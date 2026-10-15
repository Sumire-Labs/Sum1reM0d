package com.sumirelabs.sum1rem0d.game;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CoinMathTest {
    @Test void exchangeConservesCoins(){var r=CoinMath.exchange(100,20,30,5,1000);assertEquals(75,r.first());assertEquals(45,r.second());assertEquals(120,r.first()+r.second());}
    @Test void pureGiftWorks(){assertEquals(new CoinMath.Exchange(0,100),CoinMath.exchange(100,0,100,0,100));}
    @Test void rejectsNegativeAndInsufficientAmounts(){assertThrows(IllegalArgumentException.class,()->CoinMath.exchange(5,5,-1,0,100));assertThrows(IllegalArgumentException.class,()->CoinMath.exchange(5,5,6,0,100));assertThrows(IllegalArgumentException.class,()->CoinMath.credit(5,-1,100));}
    @Test void refusesOverflowAndCapWithoutPartialMutation(){assertThrows(ArithmeticException.class,()->CoinMath.exchange(Long.MAX_VALUE,1,0,1,Long.MAX_VALUE));assertThrows(IllegalArgumentException.class,()->CoinMath.exchange(100,100,0,1,100));assertThrows(ArithmeticException.class,()->CoinMath.credit(Long.MAX_VALUE,1,Long.MAX_VALUE));}
    @Test void loweringCapDoesNotDestroyExistingMoney(){assertEquals(new CoinMath.Exchange(900,100),CoinMath.exchange(1000,0,100,0,500));assertThrows(IllegalArgumentException.class,()->CoinMath.credit(1000,1,500));assertEquals(1000,CoinMath.credit(1000,0,500));}
    @Test void worksExactlyBeyondDoublePrecision(){long value=9007199254740993L;assertEquals(value-1,CoinMath.exchange(value,0,1,0,Long.MAX_VALUE).first());}
}
