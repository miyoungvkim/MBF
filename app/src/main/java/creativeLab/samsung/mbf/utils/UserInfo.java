package creativeLab.samsung.mbf.utils;

public class UserInfo {
	public static String USER_DEFAULT_NAME = "친구";
	private static String UserName = "친구";
	private static int UserAge = 1;
	private static int LimitedTime = 10;

	public static synchronized String getUserName() {
		return UserName;
	}

	public static synchronized void setUserName(String UserName) {
		UserInfo.UserName = UserName;
	}

	public static synchronized int getUserAge() {
		return UserAge;
	}

	public static synchronized void setUserAge(int UserAge) {
		UserInfo.UserAge = UserAge;
	}

	public static synchronized int getLimitedTime() {
		return LimitedTime;
	}

	public static synchronized void setLimitedTime(int LimitedTime) {
		UserInfo.LimitedTime = LimitedTime;
	}
}
