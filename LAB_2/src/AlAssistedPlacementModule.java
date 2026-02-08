import java.util.HashMap;
import java.util.Map;

/**
 * Módulo para escolha de células de armazenamento (RH3).
 * Os critérios são: balanceamento, agrupamento, e distância mínima.
 * (Apenas 9 células: [1-3] x [1-3])
 */
public class AlAssistedPlacementModule {

    /**
     * Recomenda as melhores coordenadas (X, Z) para a nova palete.
     * @param storageMap O mapa de ocupação atual.
     * @param newPallet  A palete a ser armazenada (para agrupamento).
     * @return           Um mapa com "x" e "z" da célula recomendada, ou null se não houver espaço.
     */
    public Map<String, Integer> recommendCell(Pallet[][] storageMap, Pallet newPallet) {
        
        int bestX    = -1;
        int bestZ    = -1;  
        int maxScore = -1; // Procuramos a pontuação mais alta
        
        System.out.println("[RH3-ASSIST] A calcular a melhor célula para: " + newPallet.productType);

        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                
                // 1. CRITÉRIO BÁSICO: A célula tem de estar vazia.
                if (storageMap[x][z] != null) {
                    continue; // Célula Ocupada
                }
                
                int currentScore = 0;
                
                // CRITÉRIO 1: Minimizar a Distância de Viagem (Ponto de Partida é X=1, Z=1) 
                // Dou mais peso a X=1, Z=1 no que toca a distancia
                int travelDistance = Math.abs(x - 1) + Math.abs(z - 1); 
                currentScore += (4 - travelDistance) * 10; // Peso alto para distância (max 40)
                
                // CRITÉRIO 2: Agrupar por Tipo (Score = +5 se tiver vizinho do mesmo tipo) ---
                currentScore += checkNeighbors(storageMap, x, z, newPallet.productType) * 5; // (max 4 * 5 = 20)
                
                // O critério de Balanceamento (Critério 3) é simplificado pela minimização da distância no RH3,
                // que tende a usar as células mais próximas (as de baixo) primeiro.

                System.out.println("[RH3-ASSIST] Célula (" + x + "," + z + ") - Score: " + currentScore);
                
                // Seleção da Melhor Célula 
                if (currentScore > maxScore) {
                    maxScore = currentScore;
                    bestX    = x;
                    bestZ    = z;
                }
            }
        }
        
        // --- LOG da Decisão (Obrigatório no RH3) ---
        if (bestX != -1) {
            System.out.println("[RH3-ASSIST] Decisão: Célula (" + bestX + "," + bestZ + ") selecionada com Score: " + maxScore);
            Map<String, Integer> result = new HashMap<>();
            result.put("x", bestX);
            result.put("z", bestZ);
            return result;
        } else {
            System.out.println("[RH3-ASSIST] Erro: Não há células vazias disponíveis.");
            return null;
        }
    }
    
    /**
     * Verifica se os vizinhos (cima, baixo, esquerda, direita) são do mesmo tipo.
     * @return O número de vizinhos que correspondem ao tipo.
     */
    private int checkNeighbors(Pallet[][] storageMap, int x, int z, String productType) {
        int matches = 0;
        int[][] neighbors = {{x-1, z}, {x+1, z}, {x, z-1}, {x, z+1}}; // 4 direções

        for (int[] n : neighbors) {
            int nx = n[0];
            int nz = n[1];
            
            // Verifica se está dentro dos limites [1, 3]
            if (nx >= 1 && nx <= 3 && nz >= 1 && nz <= 3) {
                Pallet neighborPallet = storageMap[nx][nz];

                // Verifica se o vizinho existe e é do mesmo tipo && produto que já lá está é igual ao que eu quero guardar 
                if (neighborPallet != null && neighborPallet.productType.equals(productType)) {
                    matches++;
                }
            }
        }
        return matches;
    }
}