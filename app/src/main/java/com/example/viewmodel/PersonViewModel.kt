package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Debt
import com.example.data.model.Payment
import com.example.data.model.Person
import com.example.data.preferences.SessionManager
import com.example.data.repository.DebtRepository
import com.example.data.repository.PaymentRepository
import com.example.data.repository.PersonRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonItemState(
    val person: Person,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val netBalance: Double = 0.0, // lent - borrowed
    val activeDebtsCount: Int = 0,
    val paidDebtsCount: Int = 0,
    val paymentsCount: Int = 0,
    val lastActivityDate: Long = person.createdAt
)

data class PersonStatement(
    val person: Person,
    val debts: List<Debt> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val totalPaidReceived: Double = 0.0,
    val netBalance: Double = 0.0
)

enum class PersonFilter(val title: String) {
    ALL("الكل"),
    CREDITOR("لك عندهم"),
    DEBTOR("لهم عندك"),
    SETTLED("حسابات مسددة")
}

class PersonViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val personRepository = PersonRepository(db.personDao(), db.debtDao(), db.paymentDao())
    private val debtRepository = DebtRepository(db.debtDao())
    private val paymentRepository = PaymentRepository(db.paymentDao(), db.debtDao())

    private val _currentUserId = MutableStateFlow(sessionManager.getUserId())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filter = MutableStateFlow(PersonFilter.ALL)
    val filter: StateFlow<PersonFilter> = _filter

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Base raw flows
    private val personsFlow = _currentUserId.flatMapLatest { userId ->
        personRepository.getAllPersons(userId)
    }

    private val debtsFlow = _currentUserId.flatMapLatest { userId ->
        debtRepository.getAllDebts(userId)
    }

    private val paymentsFlow = _currentUserId.flatMapLatest { userId ->
        paymentRepository.getAllPayments(userId)
    }

    // Unified calculated persons list
    val personsWithSummary: StateFlow<List<PersonItemState>> = combine(
        personsFlow,
        debtsFlow,
        paymentsFlow,
        _searchQuery,
        _filter
    ) { personsList, debtsList, paymentsList, query, currentFilter ->
        val debtsByPerson = debtsList.groupBy { it.name.trim().lowercase() }
        val paymentsByPerson = paymentsList.groupBy { it.personName.trim().lowercase() }

        personsList.map { person ->
            val key = person.name.trim().lowercase()
            val personDebts = debtsByPerson[key] ?: emptyList()
            val personPayments = paymentsByPerson[key] ?: emptyList()

            var lent = 0.0
            var borrowed = 0.0
            var activeCount = 0
            var paidCount = 0

            for (d in personDebts) {
                if (d.status == Debt.STATUS_PAID) {
                    paidCount++
                } else {
                    activeCount++
                    if (d.type == Debt.TYPE_CREDITOR) {
                        lent += d.amount
                    } else {
                        borrowed += d.amount
                    }
                }
            }

            // Deduct payments from active debts amounts if needed or calculate net directly
            val net = lent - borrowed
            val latestDebtDate = personDebts.maxOfOrNull { it.date } ?: person.createdAt
            val latestPaymentDate = personPayments.maxOfOrNull { it.date } ?: 0L
            val lastActive = maxOf(person.createdAt, latestDebtDate, latestPaymentDate)

            PersonItemState(
                person = person,
                totalLent = lent,
                totalBorrowed = borrowed,
                netBalance = net,
                activeDebtsCount = activeCount,
                paidDebtsCount = paidCount,
                paymentsCount = personPayments.size,
                lastActivityDate = lastActive
            )
        }.filter { item ->
            val matchesQuery = if (query.isBlank()) true else {
                item.person.name.contains(query, ignoreCase = true) ||
                        item.person.phone.contains(query) ||
                        item.person.notes.contains(query, ignoreCase = true)
            }
            val matchesFilter = when (currentFilter) {
                PersonFilter.ALL -> true
                PersonFilter.CREDITOR -> item.netBalance > 0.0
                PersonFilter.DEBTOR -> item.netBalance < 0.0
                PersonFilter.SETTLED -> item.activeDebtsCount == 0 && (item.paidDebtsCount > 0 || item.paymentsCount > 0)
            }
            matchesQuery && matchesFilter
        }.sortedByDescending { it.lastActivityDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected person for statement/details
    private val _selectedPerson = MutableStateFlow<Person?>(null)
    val selectedPerson: StateFlow<Person?> = _selectedPerson

    val selectedPersonStatement: StateFlow<PersonStatement?> = combine(
        _selectedPerson,
        debtsFlow,
        paymentsFlow
    ) { person, debtsList, paymentsList ->
        if (person == null) return@combine null

        val key = person.name.trim().lowercase()
        val pDebts = debtsList.filter { it.name.trim().equals(key, ignoreCase = true) }
            .sortedByDescending { it.date }
        val pPayments = paymentsList.filter { it.personName.trim().equals(key, ignoreCase = true) }
            .sortedByDescending { it.date }

        var lent = 0.0
        var borrowed = 0.0
        for (d in pDebts) {
            if (d.status == Debt.STATUS_ACTIVE) {
                if (d.type == Debt.TYPE_CREDITOR) lent += d.amount else borrowed += d.amount
            }
        }
        val totalPaid = pPayments.sumOf { it.amount }

        PersonStatement(
            person = person,
            debts = pDebts,
            payments = pPayments,
            totalLent = lent,
            totalBorrowed = borrowed,
            totalPaidReceived = totalPaid,
            netBalance = lent - borrowed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            val userId = _currentUserId.value
            personRepository.syncPersonsFromDebts(userId)
        }
    }

    fun refreshUser(userId: Long) {
        _currentUserId.value = userId
        viewModelScope.launch {
            personRepository.syncPersonsFromDebts(userId)
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilter(f: PersonFilter) { _filter.value = f }
    fun selectPerson(person: Person?) { _selectedPerson.value = person }

    fun addPerson(name: String, phone: String, notes: String, onDone: () -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _snackbarEvent.emit("يرجى إدخال اسم الشخص أو الجهة") }
            return
        }

        viewModelScope.launch {
            val newPerson = Person(
                userId = _currentUserId.value,
                name = trimmed,
                phone = phone.trim(),
                notes = notes.trim()
            )
            val res = personRepository.savePerson(newPerson)
            res.fold(
                onSuccess = {
                    _snackbarEvent.emit("تمت إضافة الشخص إلى دليلك بنجاح ✓")
                    onDone()
                },
                onFailure = {
                    _snackbarEvent.emit(it.message ?: "فشل حفظ بيانات الشخص")
                }
            )
        }
    }

    fun updatePerson(person: Person, newName: String, newPhone: String, newNotes: String, onDone: () -> Unit) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            viewModelScope.launch { _snackbarEvent.emit("الاسم لا يمكن أن يكون فارغاً") }
            return
        }

        viewModelScope.launch {
            val updated = person.copy(name = trimmedName, phone = newPhone.trim(), notes = newNotes.trim())
            val res = personRepository.updatePersonWithRename(person.name, updated)
            res.fold(
                onSuccess = {
                    _snackbarEvent.emit("تم تحديث بيانات الشخص وربط كافة معاملاته السابقة بنجاح ✓")
                    if (_selectedPerson.value?.id == person.id) {
                        _selectedPerson.value = updated
                    }
                    onDone()
                },
                onFailure = {
                    _snackbarEvent.emit(it.message ?: "فشل تحديث البيانات")
                }
            )
        }
    }

    fun deletePerson(person: Person, deleteDebts: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = personRepository.deletePerson(person, deleteDebts)
            res.fold(
                onSuccess = {
                    _snackbarEvent.emit("تم حذف الشخص بنجاح")
                    if (_selectedPerson.value?.id == person.id) {
                        _selectedPerson.value = null
                    }
                    onDone()
                },
                onFailure = {
                    _snackbarEvent.emit(it.message ?: "فشل الحذف")
                }
            )
        }
    }

    fun settlePersonAccount(personName: String) {
        viewModelScope.launch {
            try {
                debtRepository.settleDebtsByPerson(personName)
                _snackbarEvent.emit("تمت تسوية واعتماد سداد جميع معاملات ($personName) بنجاح ✓")
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل تسوية الحساب: ${e.message}")
            }
        }
    }
}
