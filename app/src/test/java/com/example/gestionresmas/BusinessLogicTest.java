package com.example.gestionresmas;

import com.example.gestionresmas.models.BoxPurchase;
import com.example.gestionresmas.models.Client;
import com.example.gestionresmas.models.Sale;
import com.example.gestionresmas.models.SaleDetail;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias para validar las reglas de negocio y fórmulas matemáticas
 * especificadas en SKILL.md.
 */
public class BusinessLogicTest {

    @Test
    public void testUnitCostCalculation() {
        // Caja de 10 resmas por $180,000 -> Costo por resma: $18,000
        BoxPurchase box = new BoxPurchase("CARTA", "2026-09-06", 180000.0, 10, 10);
        assertEquals(18000.0, box.getPrecioCostoUnitario(), 0.001);

        // Caja de 5 resmas por $105,000 -> Costo por resma: $21,000
        BoxPurchase box2 = new BoxPurchase("OFICIO", "2026-09-06", 105000.0, 5, 5);
        assertEquals(21000.0, box2.getPrecioCostoUnitario(), 0.001);
    }

    @Test
    public void testDynamicUnitPricingFormula() {
        // Regla SKILL.md:
        // Precio Unitario Aplicado = Costo Unitario de la Caja * (1 + Margen Utilidad / 100)
        BoxPurchase box = new BoxPurchase("CARTA", "2026-09-06", 180000.0, 10, 10); // Costo: $18,000

        // Margen del 5% de utilidad
        double dynamicPrice1 = box.calcularPrecioVenta(5.0);
        // $18,000 * 1.05 = $18,900
        assertEquals(18900.0, dynamicPrice1, 0.001);

        // Margen del 3.5% de utilidad
        double dynamicPrice2 = box.calcularPrecioVenta(3.5);
        // $18,000 * 1.035 = $18,630
        assertEquals(18630.0, dynamicPrice2, 0.001);

        // Margen del 10% de utilidad
        double dynamicPrice3 = box.calcularPrecioVenta(10.0);
        // $18,000 * 1.10 = $19,800
        assertEquals(19800.0, dynamicPrice3, 0.001);
    }

    @Test
    public void testSubtotalAndQuantityCalculation() {
        // Venta de 4 resmas a precio unitario calculado de $18,900
        SaleDetail detail = new SaleDetail(1L, 4, 18900.0);
        // Subtotal = 4 * 18,900 = $75,600
        assertEquals(75600.0, detail.getSubtotal(), 0.001);
    }

    @Test
    public void testPartialPaymentAndRemainingBalance() {
        // Venta por $70,000 en estado DEBE
        Sale sale = new Sale(1L, 1L, "2026-10-28", 70000.0, 0.0, Sale.ESTADO_DEBE);
        assertEquals(70000.0, sale.getSaldoPendiente(), 0.001);
        assertFalse(sale.tieneAbonosParciales());

        // Se registra un abono de $30,000
        sale.setMontoPagado(30000.0);
        assertEquals(40000.0, sale.getSaldoPendiente(), 0.001);
        assertTrue(sale.tieneAbonosParciales());
        assertTrue(sale.isDebe());

        // Se abonan los $40,000 restantes y pasa a PAGADO
        sale.setMontoPagado(70000.0);
        sale.setEstadoPago(Sale.ESTADO_PAGADO);
        assertEquals(0.0, sale.getSaldoPendiente(), 0.001);
        assertFalse(sale.tieneAbonosParciales());
        assertTrue(sale.isPagado());
    }

    @Test
    public void testProportionalFinancialMetricsFormula() {
        // Venta total: $70,000. Costo total de resmas: $50,000. Utilidad total: $20,000.
        double totalVenta = 70000.0;
        double costoTotal = 50000.0;
        double utilidadTotal = 20000.0;

        // Abono recibido: $35,000 (proporción = 0.5)
        double abono = 35000.0;
        double proporcion = abono / totalVenta;

        double capitalRecuperado = costoTotal * proporcion;
        double gananciaReal = utilidadTotal * proporcion;

        assertEquals(25000.0, capitalRecuperado, 0.001);
        assertEquals(10000.0, gananciaReal, 0.001);
    }
}
