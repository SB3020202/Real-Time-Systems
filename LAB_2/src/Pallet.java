import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Classe de dados para armazenar os metadados de uma palete.
 */
public class Pallet {
    
    // --- Variáveis (Metadados) ---
    String productType;
    double humidity;
    String producerID;
    String destination;
    String shippingDate; // Formato esperado: "DD/MM/AAAA"

    /**
     * Construtor para criar uma nova palete.
     */
    public Pallet(String productType, double humidity, String producerID, String destination, String shippingDate) {
        this.productType  = productType;
        this.humidity     = humidity;
        this.producerID   = producerID;
        this.destination  = destination;
        this.shippingDate = shippingDate;
    }

    /**
     * (RH6): Verifica se a data de envio da palete já passou.
     * @return true se a data de envio for hoje ou já passou, false caso contrário.
     */
    public boolean isShippingDateDue() {
        if (this.shippingDate == null || this.shippingDate.isEmpty()) {
            return false;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate shipDate = LocalDate.parse(this.shippingDate, formatter);
            LocalDate today    = LocalDate.now();
            
            // Retorna true se a data for hoje ou anterior
            return shipDate.isBefore(today) || shipDate.isEqual(today);
            
        } catch (DateTimeParseException e) {
            System.err.println("[PALLET] Erro ao analisar a data: " + this.shippingDate);
            return false;
        }
    }
    
    /**
     * (RH6): Verifica se a humidade excede o limite.
     * @param threshold O limite (ex: 50.0 para 50%)
     * @return true se a humidade for superior ao limite.
     */
    public boolean isHumidityTooHigh(double threshold) {
        return this.humidity > threshold;
    }

    @Override
    public String toString() {
        return "Palete [Tipo=" + productType + ", IDProdutor=" + producerID + "]";
    }
}