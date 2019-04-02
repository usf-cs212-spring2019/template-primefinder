import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("javadoc")
public class PrimeFinderTest {

	public static final Duration GLOBAL_TIMEOUT = Duration.ofSeconds(60);

	public static final int WARMUP_ROUNDS = 10;
	public static final int TIMED_ROUNDS = 20;

	public static final Set<Integer> KNOWN_PRIMES = Set.of(new Integer[] { 2, 3, 5, 7, 11, 13, 17, 19,
			23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113,
			127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227,
			229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337,
			347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449,
			457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577,
			587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691,
			701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827,
			829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953, 967,
			971, 977, 983, 991, 997 });

	@Test
	public void testTrialDivision() {
		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> actual = PrimeFinder.trialDivision(1, 1000);
			Assertions.assertEquals(KNOWN_PRIMES, actual);
		});
	}

	@Test
	public void testFindPrimes1Thread() {
		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> actual = PrimeFinder.findPrimes(1, 1000, 1);
			Assertions.assertEquals(KNOWN_PRIMES, actual);
		});
	}

	@Test
	public void testFindPrimes2Thread() {
		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> actual = PrimeFinder.findPrimes(1, 1000, 2);
			Assertions.assertEquals(KNOWN_PRIMES, actual);
		});
	}

	@Test
	public void testFindPrimes5Thread() {
		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> actual = PrimeFinder.findPrimes(1, 1000, 5);
			Assertions.assertEquals(KNOWN_PRIMES, actual);
		});
	}

	@Test
	public void testSingleVersusMulti() {
		int max = 3000;
		int threads = 5;

		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			TreeSet<Integer> expected = PrimeFinder.trialDivision(1, max);
			TreeSet<Integer> actual = PrimeFinder.findPrimes(1, max, threads);

			Assertions.assertEquals(expected, actual);
		});
	}

	@Test
	public void benchmarkSingleVersusMulti() {
		int max = 5000;
		int threads = 5;

		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			double single = new SingleBenchmarker().benchmark(max);
			double multi = new MultiBenchmarker(threads).benchmark(max);

			String debug = String.format("Single: %.4f Multi: %.4f, Speedup: %.4fx", single, multi,
					single / multi);

			Assertions.assertTrue(single >= multi, debug);
			System.out.println(debug);
		});
	}

	@Test
	public void benchmarkOneVersusThree() {
		int max = 5000;

		Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
			double multi1 = new MultiBenchmarker(1).benchmark(max);
			double multi3 = new MultiBenchmarker(3).benchmark(max);

			String debug = String.format("1 Thread: %.4f 3 Threads: %.4f, Speedup: %.4fx", multi1, multi3,
					multi1 / multi3);

			Assertions.assertTrue(multi1 > multi3, debug);
			System.out.println(debug);
		});
	}

	@Test
	public void testWorkQueue() throws InterruptedException {
		Assertions.assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
			WorkQueue queue = new WorkQueue();
			CountDownLatch count = new CountDownLatch(10);

			for (int i = 0; i < 10; i++) {
				queue.execute(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(10);
							count.countDown();
						}
						catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
				});
			}

			queue.finish();
			queue.shutdown();

			// if you get stuck here then finish() isn't working
			count.await();
		});
	}

	private static abstract class Benchmarker {

		public abstract void run(int max);

		public double benchmark(int max) {
			// warmup
			for (int i = 0; i < WARMUP_ROUNDS; i++) {
				run(max);
			}

			// timed
			Instant start = Instant.now();
			for (int i = 0; i < TIMED_ROUNDS; i++) {
				run(max);
			}
			Instant end = Instant.now();

			// averaged result
			Duration elapsed = Duration.between(start, end);
			return (double) elapsed.toMillis() / TIMED_ROUNDS;
		}
	}

	private static class SingleBenchmarker extends Benchmarker {

		@Override
		public void run(int max) {
			PrimeFinder.trialDivision(1, max);
		}

	}

	private static class MultiBenchmarker extends Benchmarker {

		private final int threads;

		public MultiBenchmarker(int threads) {
			this.threads = threads;
		}

		@Override
		public void run(int max) {
			PrimeFinder.findPrimes(1, max, threads);
		}
	}

}
