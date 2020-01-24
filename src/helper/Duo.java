package helper;

//http://stackoverflow.com/a/677248
public class Duo<A, B> {
	public final A first;
    public final B second;

    public Duo(A first, B second) {
    	super();
    	this.first = first;
    	this.second = second;
    }

    public int hashCode() {
    	int hashFirst = first != null ? first.hashCode() : 0;
    	int hashSecond = second != null ? second.hashCode() : 0;
    	return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    public boolean equals(Object other) {
    	if (other instanceof Duo<?, ?>) {
			@SuppressWarnings("unchecked")
			Duo<A, B> otherPair = (Duo<A, B>) other;
    		return 
    		((  this.first == otherPair.first ||
    			( this.first != null && otherPair.first != null &&
    			  this.first.equals(otherPair.first))) &&
    		 (	this.second == otherPair.second ||
    			( this.second != null && otherPair.second != null &&
    			  this.second.equals(otherPair.second))) );
    	}

    	return false;
    }

    public String toString()
    { 
    	return "(" + first + ", " + second + ")"; 
    }
}