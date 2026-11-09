package sa.factory.multaqahubcoworkin.data.repository

import sa.factory.multaqahubcoworkin.domain.repository.MultaqaHubRepository
import sa.factory.multaqahubcoworkin.hardware.HardwareSensorDriver
import sa.factory.multaqahubcoworkin.model.MultaqaHubItem
import sa.factory.multaqahubcoworkin.model.MultaqaHubOrder
import sa.factory.multaqahubcoworkin.model.MultaqaHubTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MultaqaHubRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : MultaqaHubRepository {

    private val _catalog = MutableStateFlow<List<MultaqaHubItem>>(listOf(
        MultaqaHubItem("1", "أولوية المهام بالذكاء الاصطناعي", "تحليل تلقائي لأولويات المهام عبر تكامل أنظمة AI مع أدواتك الحالية", 150.0, "معتمد", "إدارة المشاريع"),
        MultaqaHubItem("2", "إعداد بوابات RFID", "ربط نظام التحكم بالدخول 13.56MHz مع إدارة المشاريع", 280.0, "فوري", "تتبع المهام"),
        MultaqaHubItem("3", "دمج TOTP مزدوج", "مصادقة ثنائية متزامنة مع تطبيقات المصادقة HF/NFC", 95.0, "جديد", "الوصول الآمن")
    ))
    override val catalogFlow: Flow<List<MultaqaHubItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<MultaqaHubItem>>(emptyList())
    override val activeCart: StateFlow<List<MultaqaHubItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<MultaqaHubTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            MultaqaHubTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: MultaqaHubItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: MultaqaHubItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): MultaqaHubOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = MultaqaHubOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
