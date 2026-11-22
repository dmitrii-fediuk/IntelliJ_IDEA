// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
package df.idea.support;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
public final class Assertions {
	private int passed = 0;
	public void assertEquals(String label, @Nullable Object expected, @Nullable Object actual) {
		if (!Objects.equals(expected, actual)) {
			throw new AssertionError("FAIL [" + label + "]: expected=<" + expected + "> actual=<" + actual + ">");
		}
		passed++;
		System.out.println("PASS: " + label + " -> " + actual);
	}
	public void assertTrue(String label, boolean condition) {
		if (!condition) {
			throw new AssertionError("FAIL [" + label + "]");
		}
		passed++;
		System.out.println("PASS: " + label);
	}
	public int passedCount() {
		return passed;
	}
}
