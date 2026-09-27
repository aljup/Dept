package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Category
import com.example.data.model.Debt
import com.example.data.model.User
import com.example.data.preferences.SessionManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.DebtRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PersonSummary(
    val name: String,
    val totalLent: Double,        // لك عنده (مستحق لك)
    val totalBorrowed: Double,    // له عندك (التزام عليك)
    val netBalance: Double,       // الصافي = لك - عليك
    val activeCount: Int,
    val paidCount: Int,
    val totalCount: Int
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val debtRepository = DebtRepository(db.debtDao())
    private val categoryRepository = CategoryRepository(db.categoryDao(), db.debtDao())
    private val authRepository = AuthRepository(db.userDao(), SessionManager(application))

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // 1. Categories
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. All system debts
    val allDebts: StateFlow<List<Debt>> = debtRepository.getAllDebtsSystemWide()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Search & filters for admin debts view
    private val _debtsSearchQuery = MutableStateFlow("")
    val debtsSearchQuery: StateFlow<String> = _debtsSearchQuery

    private val _selectedStatusFilter = MutableStateFlow<String?>(null) // null = all, "ACTIVE", "PAID"
    val selectedStatusFilter: StateFlow<String?> = _selectedStatusFilter

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter

    val filteredDebts: StateFlow<List<Debt>> = combine(
        allDebts,
        _debtsSearchQuery,
        _selectedStatusFilter,
        _selectedCategoryFilter
    ) { debts, query, status, category ->
        debts.filter { debt ->
            val matchesQuery = if (query.isBlank()) true else {
                debt.name.contains(query, ignoreCase = true) ||
                        debt.notes.contains(query, ignoreCase = true) ||
                        debt.category.contains(query, ignoreCase = true) ||
                        debt.amount.toString().contains(query)
            }
            val matchesStatus = status == null || debt.status == status
            val matchesCategory = category == null || debt.category == category
            matchesQuery && matchesStatus && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Persons Summaries computed from all debts
    private val _personSearchQuery = MutableStateFlow("")
    val personSearchQuery: StateFlow<String> = _personSearchQuery

    val persons: StateFlow<List<PersonSummary>> = combine(allDebts, _personSearchQuery) { debts, query ->
        val map = mutableMapOf<String, MutableList<Debt>>()
        for (d in debts) {
            val trimmedName = d.name.trim()
            if (trimmedName.isNotEmpty()) {
                map.getOrPut(trimmedName) { mutableListOf() }.add(d)
            }
        }

        map.map { (name, personDebts) ->
            var lent = 0.0
            var borrowed = 0.0
            var active = 0
            var paid = 0

            for (d in personDebts) {
                if (d.status == Debt.STATUS_PAID) {
                    paid++
                } else {
                    active++
                    if (d.type == Debt.TYPE_CREDITOR) {
                        lent += d.amount
                    } else {
                        borrowed += d.amount
                    }
                }
            }

            PersonSummary(
                name = name,
                totalLent = lent,
                totalBorrowed = borrowed,
                netBalance = lent - borrowed,
                activeCount = active,
                paidCount = paid,
                totalCount = personDebts.size
            )
        }.filter {
            if (query.isBlank()) true else it.name.contains(query, ignoreCase = true)
        }.sortedByDescending { it.totalCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Users management
    val allUsers: StateFlow<List<User>> = authRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
        }
    }

    // Debts filter actions
    fun setDebtsSearchQuery(q: String) { _debtsSearchQuery.value = q }
    fun setStatusFilter(s: String?) { _selectedStatusFilter.value = s }
    fun setCategoryFilter(c: String?) { _selectedCategoryFilter.value = c }
    fun setPersonSearchQuery(q: String) { _personSearchQuery.value = q }

    // Category actions
    fun addCategory(name: String) {
        viewModelScope.launch {
            val res = categoryRepository.addCategory(name)
            res.fold(
                onSuccess = { _snackbarEvent.emit("تمت إضافة التصنيف بنجاح") },
                onFailure = { _snackbarEvent.emit(it.message ?: "فشل إضافة التصنيف") }
            )
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            val res = categoryRepository.renameCategory(category, newName)
            res.fold(
                onSuccess = { _snackbarEvent.emit("تم تعديل اسم التصنيف وتحديث المعاملات المرتبطة") },
                onFailure = { _snackbarEvent.emit(it.message ?: "فشل تعديل التصنيف") }
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val res = categoryRepository.deleteCategory(category)
            res.fold(
                onSuccess = { _snackbarEvent.emit("تم حذف التصنيف وإعادة تعيين المعاملات المرتبطة إلى 'عام'") },
                onFailure = { _snackbarEvent.emit(it.message ?: "فشل حذف التصنيف") }
            )
        }
    }

    // Person actions
    fun renamePerson(oldName: String, newName: String) {
        viewModelScope.launch {
            try {
                debtRepository.renamePerson(oldName, newName)
                _snackbarEvent.emit("تم تحديث اسم الشخص في جميع المعاملات بنجاح")
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل تحديث الاسم: ${e.message}")
            }
        }
    }

    fun settleAllDebtsForPerson(personName: String) {
        viewModelScope.launch {
            try {
                debtRepository.settleDebtsByPerson(personName)
                _snackbarEvent.emit("تمت تسوية واعتماد سداد جميع معاملات ($personName) بنجاح ✓")
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل تسوية الحساب: ${e.message}")
            }
        }
    }

    fun deleteAllDebtsForPerson(personName: String) {
        viewModelScope.launch {
            try {
                debtRepository.deleteDebtsByPerson(personName)
                _snackbarEvent.emit("تم حذف جميع المعاملات الخاصة بـ ($personName) بنجاح")
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل الحذف: ${e.message}")
            }
        }
    }

    // Single Debt actions
    fun toggleDebtStatus(debt: Debt) {
        viewModelScope.launch {
            try {
                debtRepository.toggleDebtStatus(debt)
                val msg = if (debt.status == Debt.STATUS_ACTIVE) "تم تحديد المعاملة كمسددة ✓" else "تمت إعادة المعاملة إلى الحالة النشطة"
                _snackbarEvent.emit(msg)
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل تحديث الحالة: ${e.message}")
            }
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            try {
                debtRepository.deleteDebt(debt)
                _snackbarEvent.emit("تم حذف المعاملة المالية")
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل الحذف: ${e.message}")
            }
        }
    }

    // Bulk actions
    fun bulkSettleDisplayed() {
        val currentList = filteredDebts.value
        if (currentList.isEmpty()) return
        viewModelScope.launch {
            try {
                for (d in currentList) {
                    if (d.status != Debt.STATUS_PAID) {
                        debtRepository.updateDebt(d.copy(status = Debt.STATUS_PAID))
                    }
                }
                _snackbarEvent.emit("تمت تسوية ${currentList.size} معاملة كمسددة بنجاح ✓")
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل التسوية الجماعية: ${e.message}")
            }
        }
    }

    // User actions
    fun resetUserPin(userId: Long) {
        viewModelScope.launch {
            try {
                authRepository.resetUserPin(userId, null)
                _snackbarEvent.emit("تمت إزالة رمز PIN لهذا المستخدم بنجاح")
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل إعادة التعيين: ${e.message}")
            }
        }
    }

    fun deleteUser(user: User, currentUserId: Long) {
        if (user.id == currentUserId) {
            viewModelScope.launch { _snackbarEvent.emit("لا يمكنك حذف الحساب المشرف الحالي الذي قمت بتسجيل الدخول منه!") }
            return
        }
        viewModelScope.launch {
            try {
                authRepository.deleteUser(user.id)
                debtRepository.deleteDebtsByUserId(user.id)
                _snackbarEvent.emit("تم حذف المستخدم وكافة بياناته المرتبطة بنجاح")
            } catch (e: Exception) {
                _snackbarEvent.emit("فشل حذف المستخدم: ${e.message}")
            }
        }
    }

    // Comprehensive text financial report generator
    fun generateComprehensiveReport(): String {
        val debts = allDebts.value
        val numFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
        }
        val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        val totalLent = debts.filter { it.type == Debt.TYPE_CREDITOR && it.status == Debt.STATUS_ACTIVE }.sumOf { it.amount }
        val totalBorrowed = debts.filter { it.type == Debt.TYPE_DEBTOR && it.status == Debt.STATUS_ACTIVE }.sumOf { it.amount }
        val totalPaid = debts.filter { it.status == Debt.STATUS_PAID }.sumOf { it.amount }
        val net = totalLent - totalBorrowed

        val sb = StringBuilder()
        sb.appendLine("📊 **التقرير المالي الشامل - نظام ديوني**")
        sb.appendLine("تاريخ التقرير: ${dateFormatter.format(Date())}")
        sb.appendLine("----------------------------------------")
        sb.appendLine("• إجمالي المستحقات لك (نشط): ${numFormatter.format(totalLent)}")
        sb.appendLine("• إجمالي الالتزامات عليك (نشط): ${numFormatter.format(totalBorrowed)}")
        sb.appendLine("• صافي الرصيد المالي: ${if (net >= 0) "+" else ""}${numFormatter.format(net)}")
        sb.appendLine("• إجمالي المبالغ المسددة: ${numFormatter.format(totalPaid)}")
        sb.appendLine("• إجمالي عدد المعاملات المسجلة: ${debts.size}")
        sb.appendLine("----------------------------------------")
        sb.appendLine("📋 **تفاصيل المعاملات المالية:**")

        debts.forEachIndexed { index, d ->
            val typeStr = if (d.type == Debt.TYPE_CREDITOR) "لك" else "عليك"
            val statusStr = if (d.status == Debt.STATUS_PAID) "مسدد ✓" else "نشط"
            sb.appendLine("${index + 1}. [${typeStr}] ${d.name} : ${numFormatter.format(d.amount)} (${d.category}) - ${statusStr}")
            if (d.notes.isNotBlank()) sb.appendLine("   ملاحظة: ${d.notes}")
        }

        return sb.toString()
    }
}
