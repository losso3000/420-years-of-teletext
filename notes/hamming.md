# Hamming encoding

From https://github.com/ali1234/raspi-teletext/blob/master/hamming.c

    uint8_t hamming84(uint8_t d)
    {
        uint8_t d1 = d&1;
        uint8_t d2 = (d>>1)&1;
        uint8_t d3 = (d>>2)&1;
        uint8_t d4 = (d>>3)&1;
    
        uint8_t p1 = (1 + d1 + d3 + d4) & 1;
        uint8_t p2 = (1 + d1 + d2 + d4) & 1;
        uint8_t p3 = (1 + d1 + d2 + d3) & 1;
        uint8_t p4 = (1 + p1 + d1 + p2 + d2 + p3 + d3 + d4) & 1;
    
        return (
           p1 |
          (d1<<1) |
          (p2<<2) |
          (d2<<3) |
          (p3<<4) |
          (d3<<5) |
          (p4<<6) |
          (d4<<7)
        );
    }

In Kotlin:

    fun unham(b:Int): Int {
        val d1 = if (b and 0b00000010 == 0) 0 else 0b0001
        val d2 = if (b and 0b00001000 == 0) 0 else 0b0010
        val d3 = if (b and 0b00100000 == 0) 0 else 0b0100
        val d4 = if (b and 0b10000000 == 0) 0 else 0b1000
        return d1 or d2 or d3 or d4
    }
    
    fun ham(d:Int): Int {
        val d1 = d and 1
        val d2 = d shr 1 and 1
        val d3 = d shr 2 and 1
        val d4 = d shr 3 and 1
    
        val p1 = 1 + d1 + d3 + d4 and 1
        val p2 = 1 + d1 + d2 + d4 and 1
        val p3 = 1 + d1 + d2 + d3 and 1
        val p4 = 1 + p1 + d1 + p2 + d2 + p3 + d3 + d4 and 1
    
        return p1 or
                (d1 shl 1) or
                (p2 shl 2) or
                (d2 shl 3) or
                (p3 shl 4) or
                (d3 shl 5) or
                (p4 shl 6) or
                (d4 shl 7)
    
    }

