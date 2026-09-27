package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Debt
import com.example.data.model.DebtWithPayments
import com.example.data.model.LedgerEntry
import com.example.data.model.Payment
import com.example.data.preferences.SessionManager
import com.example.data.repository.DebtRepository
import com.example.data.repository.PaymentRepository
import com.example.notification.DebtReminderScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DebtFilter(val title: String) {
    ALL("الكل"),
    CREDITOR("لك (مستحقات)"),
    DEBTOR("عليك (ديون)"),
    ACTIVE("النشطة"),
    PAID("المسددة")
}

data class DebtStats(
    val totalLent: Double = 0.0,         // لك - نشط
    val totalBorrowed: Double = 0.0,     // عليك - نشط
    val netBalance: Double = 0.0,        // الرصيد الصافي = لك - عليك
    val totalPaid: Double = 0.0,         // إجمالي المبالغ المسددة
    val totalOverall: Double = 0.0,      // الإجمالي الكلي لجميع المعاملات
    val activeCount: Int = 0,
    val paidCount: Int = 0,
    val lentCount: Int = 0,
    val borrowedCount: Int = 0,
    val paidRate: Float = 0.0f           // نسبة السداد من 0.0 إلى 1.0
)

data class DebtFormState(
    val editingId: Long? = null,
    val name: String = "",
    val amount: String = "",
    val type: String = Debt.TYPE_CREDITOR, // default: لك (أقرضت)
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String = "",
    val category: String = "شخصي",
    val status: String = Debt.STATUS_ACTIVE,
    val nameError: String? = null,
    val amountError: String? = null,
    val isSubmitting: Boolean = false
)

class DebtViewModel(application: Application) : AndroidViewModel(application) {

    private val debtRepository: DebtRepository
    private val paymentRepository: PaymentRepository
    private val sessionManager = SessionManager(application)

    private val _currentUserId = MutableStateFlow(sessionManager.getUserId())
    private val _filter = MutableStateFlow(DebtFilter.ALL)
    val filter: StateFlow<DebtFilter> = _filter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _formState = MutableStateFlow(DebtFormState())
    val formState: StateFlow<DebtFormState> = _formState

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Base raw debts flow for current user
    private val rawDebts = _currentUserId.flatMapLatest { userId ->
        debtRepository.getAllDebts(userId)
    }

    // Base raw payments flow for current user
    private val rawPayments = _currentUserId.flatMapLatest { userId ->
        paymentRepository.getAllPayments(userId)
    }

    val allPayments: StateFlow<List<Payment>> = rawPayments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered debts according to tab, search query, and category
    val debts: StateFlow<List<Debt>> = combine(
        rawDebts,
        _filter,
        _searchQuery,
        _selectedCategory
    ) { allDebts, currentFilter, query, category ->
        allDebts.filter { debt ->
            // Filter by type or status
            val matchesFilter = when (currentFilter) {
                DebtFilter.ALL -> true
                DebtFilter.CREDITOR -> debt.type == Debt.TYPE_CREDITOR
                DebtFilter.DEBTOR -> debt.type == Debt.TYPE_DEBTOR
                DebtFilter.ACTIVE -> debt.status == Debt.STATUS_ACTIVE
                DebtFilter.PAID -> debt.status == Debt.STATUS_PAID
            }

            // Filter by search query
            val matchesQuery = if (query.isBlank()) true else {
                debt.name.contains(query, ignoreCase = true) ||
                        debt.notes.contains(query, ignoreCase = true) ||
                        debt.category.contains(query, ignoreCase = true)
            }

            // Filter by category
            val matchesCategory = category == null || debt.category == category

            matchesFilter && matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Debts mapped with their partial payments & remaining balances
    val debtsWithPayments: StateFlow<List<DebtWithPayments>> = combine(debts, rawPayments) { debtsList, paymentsList ->
        val paymentsByDebtId = paymentsList.groupBy { it.debtId }
        debtsList.map { debt ->
            DebtWithPayments.from(debt, paymentsByDebtId[debt.id] ?: emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -------------------------------------------------------------
    // Payments Log (سجل الدفعات)
    // -------------------------------------------------------------
    private val _paymentsSearchQuery = MutableStateFlow("")
    val paymentsSearchQuery: StateFlow<String> = _paymentsSearchQuery

    private val _paymentMethodFilter = MutableStateFlow<String?>(null)
    val paymentMethodFilter: StateFlow<String?> = _paymentMethodFilter

    val filteredPayments: StateFlow<List<Payment>> = combine(
        allPayments,
        _paymentsSearchQuery,
        _paymentMethodFilter
    ) { payments, q, method ->
        payments.filter { p ->
            val matchesQ = if (q.isBlank()) true else {
                p.personName.contains(q, ignoreCase = true) ||
                        p.notes.contains(q, ignoreCase = true) ||
                        p.amount.toString().contains(q)
            }
            val matchesMethod = method == null || p.method == method
            matchesQ && matchesMethod
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -------------------------------------------------------------
    // Transactions Ledger / Audit Trail (سجل المعاملات الشامل)
    // -------------------------------------------------------------
    private val _ledgerSearchQuery = MutableStateFlow("")
    val ledgerSearchQuery: StateFlow<String> = _ledgerSearchQuery

    val ledgerEntries: StateFlow<List<LedgerEntry>> = combine(
        rawDebts,
        rawPayments,
        _ledgerSearchQuery
    ) { debtsList, paymentsList, query ->
        val debtsMap = debtsList.associateBy { it.id }
        val entries = mutableListOf<LedgerEntry>()

        for (d in debtsList) {
            entries.add(LedgerEntry.DebtEntry(d))
        }
        for (p in paymentsList) {
            val debt = debtsMap[p.debtId]
            entries.add(LedgerEntry.PaymentEntry(payment = p, debtType = debt?.type))
        }

        entries.filter { entry ->
            if (query.isBlank()) true else {
                entry.personName.contains(query, ignoreCase = true) ||
                        entry.notes.contains(query, ignoreCase = true) ||
                        entry.badge.contains(query, ignoreCase = true) ||
                        entry.amount.toString().contains(query)
            }
        }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Summary statistics derived from raw debts and recorded payments
    val stats: StateFlow<DebtStats> = combine(rawDebts, rawPayments) { allDebts, allPaymentsList ->
        var lent = 0.0
        var borrowed = 0.0
        var paid = 0.0
        var activeCount = 0
        var paidCount = 0
        var lentCount = 0
        var borrowedCount = 0

        val paymentsByDebt = allPaymentsList.groupBy { it.debtId }

        for (d in allDebts) {
            val debtPayments = paymentsByDebt[d.id] ?: emptyList()
            val debtPaid = debtPayments.sumOf { it.amount }

            if (d.status == Debt.STATUS_PAID) {
                paid += if (debtPaid > 0.0) debtPaid else d.amount
                paidCount++
            } else {
                activeCount++
                val remaining = (d.amount - debtPaid).coerceAtLeast(0.0)
                if (d.type == Debt.TYPE_CREDITOR) {
                    lent += remaining
                    lentCount++
                } else {
                    borrowed += remaining
                    borrowedCount++
                }
                paid += debtPaid
            }
        }

        val totalOverall = lent + borrowed + paid
        val paidRate = if (totalOverall > 0) (paid / totalOverall).toFloat() else 0.0f

        DebtStats(
            totalLent = lent,
            totalBorrowed = borrowed,
            netBalance = lent - borrowed,
            totalPaid = paid,
            totalOverall = totalOverall,
            activeCount = activeCount,
            paidCount = paidCount,
            lentCount = lentCount,
            borrowedCount = borrowedCount,
            paidRate = paidRate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtStats()
    )

    init {
        val db = AppDatabase.getDatabase(application)
        debtRepository = DebtRepository(db.debtDao())
        paymentRepository = PaymentRepository(db.paymentDao(), db.debtDao())
        viewModelScope.launch {
            debtRepository.purgeSampleDebts()
            NotificationHelper.createNotificationChannel(application)
            DebtReminderScheduler.scheduleDailyPeriodicCheck(application)
        }
    }

    fun refreshUser(userId: Long) {
        _currentUserId.value = userId
    }

    fun getCurrency(): String = sessionManager.getCurrency()

    fun setFilter(filter: DebtFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(cat: String?) {
        _selectedCategory.value = cat
    }

    // Form handlers
    fun initFormForAdd(initialName: String = "", initialType: String = Debt.TYPE_CREDITOR) {
        _formState.value = DebtFormState(
            name = initialName,
            type = initialType
        )
    }

    fun initFormForEdit(debt: Debt) {
        _formState.value = DebtFormState(
            editingId = debt.id,
            name = debt.name,
            amount = if (debt.amount % 1.0 == 0.0) debt.amount.toLong().toString() else debt.amount.toString(),
            type = debt.type,
            date = debt.date,
            dueDate = debt.dueDate,
            notes = debt.notes,
            category = debt.category,
            status = debt.status
        )
    }

    fun onFormNameChange(name: String) {
        _formState.update { it.copy(name = name, nameError = null) }
    }

    fun onFormAmountChange(amount: String) {
        val sanitized = amount.filter { it.isDigit() || it == '.' }
        _formState.update { it.copy(amount = sanitized, amountError = null) }
    }

    fun onFormTypeChange(type: String) {
        _formState.update { it.copy(type = type) }
    }

    fun onFormDateChange(date: Long) {
        _formState.update { it.copy(date = date) }
    }

    fun onFormDueDateChange(dueDate: Long?) {
        _formState.update { it.copy(dueDate = dueDate) }
    }

    fun onFormNotesChange(notes: String) {
        _formState.update { it.copy(notes = notes) }
    }

    fun onFormCategoryChange(category: String) {
        _formState.update { it.copy(category = category) }
    }

    fun onFormStatusChange(status: String) {
        _formState.update { it.copy(status = status) }
    }

    fun saveDebt(onSuccess: () -> Unit) {
        val state = _formState.value
        val nameTrimmed = state.name.trim()
        val amountParsed = state.amount.toDoubleOrNull()

        var hasError = false
        if (nameTrimmed.isEmpty()) {
            _formState.update { it.copy(nameError = "يرجى إدخال اسم الشخص أو الجهة") }
            hasError = true
        }
        if (amountParsed == null || amountParsed <= 0.0) {
            _formState.update { it.copy(amountError = "يرجى إدخال مبلغ صحيح أكبر من صفر") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            val currentUserId = _currentUserId.value
            val debt = Debt(
                id = state.editingId ?: 0L,
                userId = currentUserId,
                name = nameTrimmed,
                amount = amountParsed!!,
                type = state.type,
                date = state.date,
                dueDate = state.dueDate,
                notes = state.notes.trim(),
                category = state.category,
                status = state.status
            )

            try {
                val savedId = if (state.editingId == null) {
                    val newId = debtRepository.insertDebt(debt)
                    _snackbarEvent.emit("تمت إضافة المعاملة المالية بنجاح")
                    newId
                } else {
                    debtRepository.updateDebt(debt)
                    _snackbarEvent.emit("تم تحديث بيانات المعاملة بنجاح")
                    debt.id
                }

                val updatedDebt = debt.copy(id = savedId)
                if (updatedDebt.dueDate != null && updatedDebt.status == Debt.STATUS_ACTIVE) {
                    DebtReminderScheduler.scheduleDebtReminder(getApplication(), updatedDebt)
                } else {
                    DebtReminderScheduler.cancelDebtReminder(getApplication(), updatedDebt.id)
                }

                _formState.update { it.copy(isSubmitting = false) }
                onSuccess()
            } catch (e: Exception) {
                _formState.update { it.copy(isSubmitting = false) }
                _snackbarEvent.emit("حدث خطأ أثناء الحفظ: ${e.message}")
            }
        }
    }

    fun toggleDebtStatus(debt: Debt) {
        viewModelScope.launch {
            try {
                val newStatus = if (debt.status == Debt.STATUS_ACTIVE) Debt.STATUS_PAID else Debt.STATUS_ACTIVE
                val updated = debt.copy(status = newStatus)
                debtRepository.updateDebt(updated)

                if (newStatus == Debt.STATUS_PAID) {
                    DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
                } else if (debt.dueDate != null) {
                    DebtReminderScheduler.scheduleDebtReminder(getApplication(), updated)
                }

                val msg = if (newStatus == Debt.STATUS_PAID) "تم تحديد الدين كمسدد بالكامل ✓" else "تمت إعادة الدين إلى الحالة النشطة"
                _snackbarEvent.emit(msg)
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل تحديث الحالة: ${e.message}")
            }
        }
    }

    fun deleteDebt(debt: Debt, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                debtRepository.deleteDebt(debt)
                DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
                _snackbarEvent.emit("تم حذف المعاملة بنجاح")
                onDeleted?.invoke()
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل الحذف: ${e.message}")
            }
        }
    }

    fun triggerDueDebtsReminderCheck() {
        viewModelScope.launch {
            val count = NotificationHelper.checkUpcomingDebtsAndNotify(getApplication())
            if (count > 0) {
                _snackbarEvent.emit("تم إرسال $count إشعار تذكير بمواعيد استحقاق الديون 🔔")
            } else {
                _snackbarEvent.emit("لا توجد ديون تستحق التذكير اليوم ✓")
            }
        }
    }

    // -------------------------------------------------------------
    // Payment Management Methods (إضافة / تعديل / حذف الدفعات والأقساط)
    // -------------------------------------------------------------
    fun addPayment(
        debtId: Long,
        personName: String,
        amount: Double,
        method: String,
        notes: String,
        date: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit
    ) {
        if (amount <= 0.0) {
            viewModelScope.launch { _snackbarEvent.emit("يرجى إدخال مبلغ صحيح للدفعة أكبر من صفر") }
            return
        }

        viewModelScope.launch {
            val payment = Payment(
                debtId = debtId,
                userId = _currentUserId.value,
                personName = personName.trim(),
                amount = amount,
                method = method,
                notes = notes.trim(),
                date = date
            )
            val result = paymentRepository.addPayment(payment)
            result.fold(
                onSuccess = {
                    _snackbarEvent.emit("تم تسجيل الدفعة المالية بنجاح ✓")
                    onSuccess()
                },
                onFailure = {
                    _snackbarEvent.emit("فشل تسجيل الدفعة: ${it.message}")
                }
            )
        }
    }

    fun updatePayment(
        payment: Payment,
        newAmount: Double,
        newMethod: String,
        newNotes: String,
        newDate: Long,
        onSuccess: () -> Unit
    ) {
        if (newAmount <= 0.0) {
            viewModelScope.launch { _snackbarEvent.emit("يرجى إدخال مبلغ صحيح للدفعة") }
            return
        }

        viewModelScope.launch {
            val updated = payment.copy(
                amount = newAmount,
                method = newMethod,
                notes = newNotes.trim(),
                date = newDate
            )
            val result = paymentRepository.updatePayment(updated)
            result.fold(
                onSuccess = {
                    _snackbarEvent.emit("تم تعديل بيانات الدفعة بنجاح ✓")
                    onSuccess()
                },
                onFailure = {
                    _snackbarEvent.emit("فشل تعديل الدفعة: ${it.message}")
                }
            )
        }
    }

    fun deletePayment(payment: Payment, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = paymentRepository.deletePayment(payment)
            result.fold(
                onSuccess = {
                    _snackbarEvent.emit("تم حذف الدفعة وتحديث الرصيد المتبقي بنجاح")
                    onDeleted?.invoke()
                },
                onFailure = {
                    _snackbarEvent.emit("فشل حذف الدفعة: ${it.message}")
                }
            )
        }
    }

    fun setPaymentsSearchQuery(q: String) { _paymentsSearchQuery.value = q }
    fun setPaymentMethodFilter(m: String?) { _paymentMethodFilter.value = m }
    fun setLedgerSearchQuery(q: String) { _ledgerSearchQuery.value = q }

    fun getPaymentsForDebtFlow(debtId: Long) = paymentRepository.getPaymentsForDebt(debtId)

    fun getDebtWithPaymentsFlow(debtId: Long): Flow<DebtWithPayments?> {
        return combine(
            debtRepository.getDebtById(debtId),
            paymentRepository.getPaymentsForDebt(debtId)
        ) { debt, payments ->
            debt?.let { DebtWithPayments.from(it, payments) }
        }
    }

    fun getDebtFlow(id: Long) = debtRepository.getDebtById(id)
}
