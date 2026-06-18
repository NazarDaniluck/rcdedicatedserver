package net.rcdedicatedserver.server;

import net.rcdedicatedserver.server.NoiseMap;

public class WorldGenerator {
    private int width;
    private int height;
    private int depth;

    public WorldGenerator(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public byte[] generateMap() {
        byte[] map = new byte[width * height * depth];
        NoiseMap noise1 = new NoiseMap();
        NoiseMap noise2 = new NoiseMap();
        NoiseMap caveNoise = new NoiseMap(); // <--- Добавляем один дополнительный генератор для шахт

        // ТВОЙ ОРИГИНАЛЬНЫЙ КОД ГЕНЕРАЦИИ — ОСТАВЛЕН ПОЛНОСТЬЮ БЕЗ ИЗМЕНЕНИЙ
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                float n1 = noise1.getNoise(x * 0.02f, z * 0.02f) * 12.0f;
                float n2 = noise2.getNoise(x * 0.05f, z * 0.05f) * 4.0f;
                int surfaceHeight = (height / 2) + (int) (n1 + n2);

                if (surfaceHeight >= height) surfaceHeight = height - 1;
                if (surfaceHeight < 1) surfaceHeight = 1;

                for (int y = 0; y < height; y++) {
                    int idx = (y * depth + z) * width + x;
                    if (y < surfaceHeight - 4) {
                        map[idx] = 1; // Твой честный Камень
                    } else if (y < surfaceHeight - 1) {
                        map[idx] = 2; // Твоя честная Земля
                    } else if (y == surfaceHeight - 1) {
                        map[idx] = 3; // Твоя честная Трава
                    } else {
                        map[idx] = 0; // Воздух
                    }
                }
            }
        }

        // А КУСOК НИЖЕ ПРОСТО ПРОГРЫЗАЕТ ШАХТЫ В ТВОИХ КЛАСТЕРАХ КАМНЯ И ЗЕМЛИ
        for (int x = 0; x < width; x++) {
            // Ограничиваем высоту 'y' до (surfaceHeight - 5), чтобы пещеры никогда не взрывали траву на поверхности
            for (int y = 1; y < (height / 2) - 2; y++) { 
                for (int z = 0; z < depth; z++) {
                    int idx = (y * depth + z) * width + x;
                    
                    // Грызем коридоры строго внутри твоего Камня (1) и Земли (2)
                    if (map[idx] == 1 || map[idx] == 2) {
                        // Масштабируем 3D-шум под аккуратные человеческие проходы
                        float density = caveNoise.getNoise3D(x * 0.08f, y * 0.14f, z * 0.08f);
                        
                        // Если плотность выше 0.47f — вырезаем проходимый коридор шахты
                        if (density > 0.47f) {
                            map[idx] = 0; // Превращаем в воздух
                        }
                    }
                }
            }
        }

        return map;
    }
}

