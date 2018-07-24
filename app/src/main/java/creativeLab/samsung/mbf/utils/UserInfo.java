package creativeLab.samsung.mbf.utils;

public class UserInfo {
	private static String UserName = "친구";
	private static int UserAge = 1;
	private static int LimitiedTime = 10;

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

	public static synchronized int getLimitiedTime() {
		return LimitiedTime;
	}

	public static synchronized void setLimitiedTime(int LimitiedTime) {
		UserInfo.LimitiedTime = LimitiedTime;
	}
}
