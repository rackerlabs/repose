package framework.category

/*
 * This interface is used to associate spock tests to a category described by the class name.
 *
 * A test is considered flaky if, given the same input (i.e., project files), it cannot be
 * determined whether the test will pass or fail.
 */
public interface Flaky {}
