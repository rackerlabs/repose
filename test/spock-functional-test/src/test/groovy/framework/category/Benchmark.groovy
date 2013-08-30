package framework.category

/*
 * This interface is used to associate spock tests to a category described by the class name.
 *
 * A test is considered a benchmark test if it is used to gauge run-time metrics.
 * In other words, a benchmark test introduces load into a system and quantitatively measures
 * the impact (e.g., throughput, response time, number of errors, etc.).
 */
public interface Benchmark extends Slow {}
