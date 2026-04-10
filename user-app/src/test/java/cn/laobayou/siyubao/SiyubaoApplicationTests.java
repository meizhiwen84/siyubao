package cn.laobayou.siyubao;

import java.math.BigInteger;

public class SiyubaoApplicationTests {
	// Base62 字符表 (0-9, A-Z, a-z)
	private static final String BASE62_CHARS =
			"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	/**
	 * 将长整型数字（如毫秒数）编码为 Base62 字符串
	 */
	public static String encode(long num) {
		BigInteger number = BigInteger.valueOf(num);
		BigInteger base = BigInteger.valueOf(64);
		StringBuilder result = new StringBuilder();

		do {
			int remainder = number.mod(base).intValue();
			result.insert(0, BASE62_CHARS.charAt(remainder));
			number = number.divide(base);
		} while (number.compareTo(BigInteger.ZERO) > 0);

		return result.toString();
	}

	public static void main(String[] args) {
		long millis = 1752120663410930L;
		millis=System.nanoTime();
		System.out.println("当前时间："+millis);
		String shortCode = encode(millis);
		System.out.println("Base62 编码结果: " + shortCode); // 输出: JLqwc9Azh
		System.out.println("最后4位: " + shortCode.substring(shortCode.length() - 4)); // 输出: 9Azh
	}
}