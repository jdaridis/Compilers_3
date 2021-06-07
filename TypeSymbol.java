public class TypeSymbol extends Symbol{


    public TypeSymbol(String id, PrimitiveType type) {
        super(id, type);
    }

    public TypeSymbol(PrimitiveType type) {
        super(type.typeName, type);
    }

	public TypeSymbol(String id) {
		super(id, PrimitiveType.IDENTIFIER);
	}

    public String getTypeName(){
        return this.id;
    }

    @Override
    public String toString() {
        return this.id;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return ((TypeSymbol)obj).id.equals(this.id);
    }

        
}
