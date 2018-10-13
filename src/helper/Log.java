package helper;

public class Log {
	//TODO class to print log messages somewhere.
	//This should really be configurable, if you want it isn't a complex file
	

	/**Easier way of typing System.out.println(); -> Log.p();*/
	public static void p() {
		System.out.println();
	}

	public static void p(Object o) {
		System.out.println(o);
	}

	public static void p(Object... os) {
		for (Object o : os) {
	        System.out.print(o + " ");
	    }
		System.out.println();
	}

	public static void p(Object[] ol, String sep) {
		if (sep == null) sep = "\n";
		for (Object o: ol)
			System.out.print(o + sep);
		System.out.println();
	}

	public static void p(Iterable<Object> ol, String sep) {
		Log.p(ol, sep);
	}

	public static void p(Object[][] matrix, String sep) {
		if (matrix == null)
			return;
		if (matrix.length == 0)
			return;
		
		System.out.println("Matrix:");
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				System.out.print(matrix[i][j] + sep);
			}
			System.out.println();
		}
	}

	/**Easier way of typing System.err.println(); -> Log.e();*/
	public static void e(Object o) {
		System.err.println(o);
	}

	public static void e(Object... os) {
		for (Object o : os) {
	        System.err.print(o + " ");
	    }
		System.err.println();
	}

	public static void e(Object[] ol, String sep) {
		if (sep == null) sep = "\n";
		for (Object o: ol)
			System.err.print(o+sep);
		System.err.println();
	}

	public static void e(Iterable<Object> ol, String sep) {
		Log.e(ol, sep);
	}

	public static void e(Exception e) {
		e.printStackTrace(System.err);
	}
}
