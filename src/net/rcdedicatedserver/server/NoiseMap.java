package net.rcdedicatedserver.server;

import java.util.Random;

public class NoiseMap {
    private int[] permutations = new int[512];

    public NoiseMap() {
        Random random = new Random();
        for (int i = 0; i < 256; i++) {
            permutations[i] = i;
        }
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int temp = permutations[i];
            permutations[i] = permutations[j];
            permutations[j] = temp;
            permutations[i + 256] = permutations[i];
        }
    }

    // Стандартный 2D шум для холмов поверхности
    public float getNoise(float x, float z) {
        return getNoise3D(x, 0.0f, z);
    }

    // ТОТ САМЫЙ МЕТОД: Мощный 3D шум для генерации извилистых пещер и шахт
    public float getNoise3D(float x, float y, float z) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;
        
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        
        float u = fade(x);
        float v = fade(y);
        float w = fade(z);
        
        int A = permutations[X] + Y;
        int AA = permutations[A] + Z;
        int AB = permutations[A + 1] + Z;
        int B = permutations[X + 1] + Y;
        int BA = permutations[B] + Z;
        int BB = permutations[B + 1] + Z;
        
        return lerp(w, lerp(v, lerp(u, grad(permutations[AA], x, y, z), 
                                        grad(permutations[BA], x - 1, y, z)),
                               lerp(u, grad(permutations[AB], x, y - 1, z), 
                                        grad(permutations[BB], x - 1, y - 1, z))),
                       lerp(v, lerp(u, grad(permutations[AA + 1], x, y, z - 1), 
                                        grad(permutations[BA + 1], x - 1, y, z - 1)),
                               lerp(u, grad(permutations[AB + 1], x, y - 1, z - 1), 
                                        grad(permutations[BB + 1], x - 1, y - 1, z - 1))));
    }

    private float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private float grad(int hash, float x, float y, float z) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
