package com.le.xxl;

import java.util.ArrayList;

public class JsonTool {

	/**
	 * prefix of ascii string of native character
	 */
	private static String PREFIX = "\\u";

	/**
	 * Native to ascii string. It's same as execut native2ascii.exe.
	 * 
	 * @param str
	 *            native string
	 * @return ascii string
	 */
	public static String native2Ascii(String str) {
		char[] chars = str.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < chars.length; i++) {
			sb.append(char2Ascii(chars[i]));
		}
		return sb.toString();
	}

	/**
	 * Native character to ascii string.
	 * 
	 * @param c
	 *            native character
	 * @return ascii string
	 */
	private static String char2Ascii(char c) {
		if (c > 255) {
			StringBuilder sb = new StringBuilder();
			sb.append(PREFIX);
			int code = (c >> 8);
			String tmp = Integer.toHexString(code);
			if (tmp.length() == 1) {
				sb.append("0");
			}
			sb.append(tmp);
			code = (c & 0xFF);
			tmp = Integer.toHexString(code);
			if (tmp.length() == 1) {
				sb.append("0");
			}
			sb.append(tmp);
			return sb.toString();
		} else {
			return Character.toString(c);
		}
	}

	/**
	 * unicode编码转化为汉字(asci编码转化为16进制编码)
	 * 
	 * @param str
	 * @return
	 */
	public static String ascii2Native(String unicodeStr) {
		StringBuilder sb = new StringBuilder();
		int begin = 0;
		int index = unicodeStr.indexOf(PREFIX);
		while (index != -1) {
			sb.append(unicodeStr.substring(begin, index));
			sb.append(ascii2Char(unicodeStr.substring(index, index + 6)));
			begin = index + 6;
			index = unicodeStr.indexOf(PREFIX, begin);
		}
		sb.append(unicodeStr.substring(begin));
		return sb.toString();
	}

	/**
	 * 将asci编码转化为16进制编码
	 * 
	 * @param unicodeStr
	 * @return
	 */
	private static char ascii2Char(String unicodeStr) {
		if (unicodeStr.length() != 6) {
			throw new IllegalArgumentException(
					"Ascii string of a native character must be 6 character.");
		}
		if (!"\\u".equals(unicodeStr.substring(0, 2))) {
			throw new IllegalArgumentException(
					"Ascii string of a native character must start with \"\\u\".");
		}
		String tmp = unicodeStr.substring(2, 4);
		int code = Integer.parseInt(tmp, 16) << 8;
		tmp = unicodeStr.substring(4, 6);
		code += Integer.parseInt(tmp, 16);
		return (char) code;
	}

	/**
	 * json字符串的格式化
	 * 
	 * @param jsonString
	 *            Json字符数据
	 * @param fillStringUnit
	 *            每一层之前的占位符号比如空格 制表符
	 * @return
	 */
	public static String formatJson(String jsonString, String fillStringUnit) {
		// 数据为null，则返回空数据
		if (jsonString == null || jsonString.trim().length() == 0) {
			return null;
		}

		// 预读取(分行)
		ArrayList<String> eachRowStringList = new ArrayList<String>();
		{
			String jsonTemp = jsonString;
			// 预读取
			while (jsonTemp.length() > 0) {
				// 获取每一行的串
				String eachRowString = getEachRowOfJsonString(jsonTemp);
				// 将此行字符串存入List当中
				eachRowStringList.add(eachRowString.trim());

				// 除去此行字符及其之前字符串后，剩余的字符串(去执行下一次循环)
				jsonTemp = jsonTemp.substring(eachRowString.length());

			}
		}

		int fixedLenth = 0;
		for (int i = 0; i < eachRowStringList.size(); i++) {
			String token = eachRowStringList.get(i);
			int length = token.getBytes().length;
			if (length > fixedLenth && i < eachRowStringList.size() - 1
					&& eachRowStringList.get(i + 1).equals(":")) {
				fixedLenth = length;
			}
		}

		StringBuilder buf = new StringBuilder();
		int count = 0;
		for (int i = 0; i < eachRowStringList.size(); i++) {

			String token = eachRowStringList.get(i);

			if (token.equals(",")) {
				buf.append(token);
				doFill(buf, count, fillStringUnit);
				continue;
			}
			if (token.equals(":")) {
				buf.append(" ").append(token).append(" ");
				continue;
			}
			if (token.equals("{")) {
				String nextToken = eachRowStringList.get(i + 1);
				if (nextToken.equals("}")) {
					i++;
					buf.append("{ }");
				} else {
					count++;
					buf.append(token);
					doFill(buf, count, fillStringUnit);
				}
				continue;
			}
			if (token.equals("}")) {
				count--;
				doFill(buf, count, fillStringUnit);
				buf.append(token);
				continue;
			}
			if (token.equals("[")) {
				String nextToken = eachRowStringList.get(i + 1);
				if (nextToken.equals("]")) {
					i++;
					buf.append("[ ]");
				} else {
					count++;
					buf.append(token);
					doFill(buf, count, fillStringUnit);
				}
				continue;
			}
			if (token.equals("]")) {
				count--;
				doFill(buf, count, fillStringUnit);
				buf.append(token);
				continue;
			}

			buf.append(token);
			// 左对齐
			if (i < eachRowStringList.size() - 1
					&& eachRowStringList.get(i + 1).equals(":")) {
				int fillLength = fixedLenth - token.getBytes().length;
				if (fillLength > 0) {
					for (int j = 0; j < fillLength; j++) {
						buf.append(" ");
					}
				}
			}
		}
		return buf.toString();
	}

	/**
	 * 获取每一行的串, { } [ ]等结尾的串
	 * 
	 * @param jsonString
	 * @return
	 */
	private static String getEachRowOfJsonString(String jsonString) {
		StringBuilder buf = new StringBuilder();
		boolean isInYinHao = false;
		while (jsonString.length() > 0) {
			// 获取json串的第一个字符
			String firstString = jsonString.substring(0, 1);
			// 如果第一个字符串是标点符号: { } [ ] ,
			if (!isInYinHao
					&& (firstString.equals(":") || firstString.equals("{")
							|| firstString.equals("}")
							|| firstString.equals("[")
							|| firstString.equals("]") || firstString
							.equals(","))) {
				// 并且此行没有数据，加入到此行当中
				if (buf.toString().trim().length() == 0) {
					buf.append(firstString);
				}
				break;
			}
			// 除第一个字符之外的其他字符
			jsonString = jsonString.substring(1);
			// 如果是汉字字符
			if (firstString.equals("\\")) {
				buf.append(firstString);
				buf.append(jsonString.substring(0, 1));
				jsonString = jsonString.substring(1);
				continue;
			}
			// 如果是引号
			if (firstString.equals("\"")) {
				buf.append(firstString);
				if (isInYinHao) {
					break;
				} else {
					isInYinHao = true;
					continue;
				}
			}
			buf.append(firstString);
		}
		return buf.toString();
	}

	private static void doFill(StringBuilder buf, int count,
			String fillStringUnit) {
		buf.append("\n");
		for (int i = 0; i < count; i++) {
			buf.append(fillStringUnit);
		}
	}

}