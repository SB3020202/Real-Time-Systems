/**
 * Classe de dados que representa um pedido de trabalho.
 */
public class StorageRequest {

    public enum RequestType {
        CALIBRATE,          // RH1 
        STORE,              // RH2/RH3
        DELIVER,            // RH4
        TOGGLE_MODE,        // Mudar Modo RH4
        DELIVER_BY_CRITERIA,// RH5
        MASS_REMOVAL,       // RH7
        EMERGENCY_STOP,     // RH8 
        RESUME,             // RH9 
        RESET,              // RH10 
        LIST_PRODUCTS,      // RH12
        LOOKUP              // RH13 
    }

    // --- Variáveis do Pedido ---
    final RequestType type;
    final Pallet      palletToStore; 
    final int         targetX;         
    final int         targetZ;
    final String      searchType;  
    final String      searchTerm; 

    /**
     * Construtor genérico para pedidos sem parâmetros (CALIBRATE, TOGGLE_MODE, MASS_REMOVAL, EMERGENCY_STOP, RESUME, RESET, LIST_PRODUCTS).
     */
    public StorageRequest(RequestType type) {
        // O construtor genérico agora aceita todos os comandos de controle de estado e o novo LIST_PRODUCTS
        if (type == RequestType.CALIBRATE    || type == RequestType.TOGGLE_MODE    || 
            type == RequestType.MASS_REMOVAL || type == RequestType.EMERGENCY_STOP || 
            type == RequestType.RESUME       || type == RequestType.RESET          ||
            type == RequestType.LIST_PRODUCTS) {
            
            this.type          = type;
            this.palletToStore = null;
            this.targetX       = 0;
            this.targetZ       = 0;
            this.searchType    = null;
            this.searchTerm    = null;
        } else {
            throw new IllegalArgumentException("Use os construtores específicos para este tipo de pedido");
        }
    }

    /**
     * Construtor para ARMAZENAR (RH2 - Manual).
     */
    public StorageRequest(Pallet pallet, int x, int z) {
        this.type          = RequestType.STORE;
        this.palletToStore = pallet;
        this.targetX       = x; 
        this.targetZ       = z; 
        this.searchType    = null; this.searchTerm = null;
    }

    /**
     * Construtor para ARMAZENAR (RH3 - Assistido).
     */
    public StorageRequest(Pallet pallet) {
        this.type          = RequestType.STORE;
        this.palletToStore = pallet;
        this.targetX       = 0; // Sinaliza Modo Assistido
        this.targetZ       = 0; // Sinaliza Modo Assistido
        this.searchType    = null; this.searchTerm = null;
    }

    /**
     * Construtor para ENTREGAR (RH4).
     */
    public StorageRequest(RequestType type, int x, int z) {
        if (type != RequestType.DELIVER) {
            throw new IllegalArgumentException("Este construtor destina-se apenas a pedidos DELIVER");
        }
        this.type          = RequestType.DELIVER;
        this.palletToStore = null; 
        this.targetX       = x; 
        this.targetZ       = z; 
        this.searchType    = null; this.searchTerm = null;
    }
    
    /**
     * Construtor para ENTREGAR POR CRITÉRIO (RH5) ou CONSULTAR (RH13).
     */
    public StorageRequest(RequestType type, String searchType, String searchTerm) {
        // Aceita DELIVER_BY_CRITERIA (RH5) e LOOKUP (RH13)
        if (type != RequestType.DELIVER_BY_CRITERIA && type != RequestType.LOOKUP) {
            throw new IllegalArgumentException("Este construtor destina-se a pedidos de CRITÉRIO (Deliver ou Lookup)");
        }
        this.type          = type;
        this.palletToStore = null;
        this.targetX       = 0; 
        this.targetZ       = 0;
        this.searchType    = searchType; 
        this.searchTerm    = searchTerm;
    }

    /**
     * Determina se o pedido é para Armazenamento Assistido (RH3).
     */
    public boolean isAssisted() {
        return this.type == RequestType.STORE && this.targetX == 0 && this.targetZ == 0;
    }
}