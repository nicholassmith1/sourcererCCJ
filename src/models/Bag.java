package models;
import java.util.LinkedHashSet;

/**
 * 
 */

/**
 * @author vaibhavsaini
 * 
 */
public class Bag extends LinkedHashSet<TokenFrequency> { // TODO: why is this not a linkedhashmap?
    /**
     * 
     */
    private static final long serialVersionUID = 1721183896451527542L;
    private String id; //converted long to String - Sarah
    private int size;
    private int comparisions;
    private String functionId; //converted long to String - Sarah

    public int getComparisions() {
		return comparisions;
	}

	public void setComparisions(int comparisions) {
		this.comparisions = comparisions;
	}

	/**
     * @param bagId
     */
    public Bag(String bagId) { 
        super();
        this.id = bagId;
        this.size =0;
        this.comparisions=0;
        this.functionId = bagId; //bagId and functionId are the same
    }
    
    public Bag(){
        super();
    }

    public String getFunctionId() {
		return functionId; //converted long to String - Sarah
	}

	public void setFunctionId(String functionId) {
		this.functionId = functionId; //converted long to String - Sarah
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bag other = (Bag) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	/**
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(int id) {
        this.id = Integer.toString(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String returnString = "";
        for (TokenFrequency tokenFrequency : this) {
            returnString += tokenFrequency.getToken().toString() + "@@::@@"
                    + tokenFrequency.getFrequency() + ",";
        }
        return this.id+ "@#@"+ returnString.substring(0,returnString.length()-1) + System.getProperty("line.separator");
    }

    
    public TokenFrequency get(TokenFrequency tokenFrequency) {
        this.comparisions=0;
        for (TokenFrequency tf : this) {
            this.comparisions+=1;
            if (tf.equals(tokenFrequency)) {
                return tf;
            }
        }
        return null;
    }
    

    public int getSize() {
        if(this.size == 0){
        	for (TokenFrequency tf : this) {
                this.size += tf.getFrequency();
            }
        }
        return this.size;
    }
}
