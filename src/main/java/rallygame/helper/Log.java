package rallygame.helper;

import java.io.PrintStream;

public class Log {
	
	// Is it worth changing these to be recursive to be able to print many lists?

	//This should really be configurable, if you want it isn't a complex file
	private static PrintStream OUT = System.out;
	private static PrintStream ERR = System.err;

	/**Easier way of typing OUT.println(); -> Log.p();*/
	public static void p() {
		OUT.println();
	}

	public static void p(Object o) {
		OUT.println(o);
	}

	public static void p(Object[] ol, String sep) {
		if (sep == null) sep = "\n";
		for (Object o: ol)
			OUT.print(o + sep);
		OUT.println();
	}
	public static void p(Object... os) {
		for (Object o : os) {
	        OUT.print(o + " ");
	    }
		OUT.println();
	}
	
	public static void p(Iterable<Object> ol, String sep) {
		p(ol, sep);
	}

	public static void p(Object[][] matrix, String sep) {
		if (matrix == null)
			return;
		if (matrix.length == 0)
			return;
		
		OUT.println("Matrix:");
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				OUT.print(matrix[i][j] + sep);
			}
			OUT.println();
		}
	}

	/**Easier way of typing ERR.println(); -> Log.e();*/
	public static void e(Object o) {
		ERR.println(o);
	}

	public static void e(Object... os) {
		for (Object o : os) {
	        ERR.print(o + " ");
	    }
		ERR.println();
	}

	public static void e(Object[] ol, String sep) {
		if (sep == null) sep = "\n";
		for (Object o: ol)
			ERR.print(o+sep);
		ERR.println();
	}

	public static void e(Iterable<Object> ol, String sep) {
		Log.e(ol, sep);
	}

	public static void e(Exception e) {
		e.printStackTrace(System.err);
    }
    
    /** System.exit() with number and message */
    public static void exit(int num, Object o) {
        Log.e(o);
        System.exit(num);
    }
}
