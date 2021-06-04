enum PrimitiveType {
    INT("i32", 4),
    ARRAY("i32*", 8),
    BOOLEAN("i1", 1),
    IDENTIFIER("i8*",8);


    String typeName;
    int size;
    PrimitiveType(int size){
        this.size = size;
    }
    PrimitiveType(String name, int size){
        this.typeName = name;
        this.size = size;
    }

    public static PrimitiveType strToPrimitiveType(String strType){
        PrimitiveType type;
        switch(strType){
            case "i32":
                type = INT;
                break;
            case "i1":
                type = BOOLEAN;
                break;
            case "i32*":
                type = ARRAY;
                break;
            default:
                type = IDENTIFIER;
                break;
        }

        
        return type;
    }
    public String getTypeName() {
        return typeName;
    }
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    public int getSize() {
        return size;
    }

}

public class Symbol {
    String id;
    PrimitiveType type;
    int size;
    ClassDeclSymbol thisSymbol;

    static int tempCount = 0;
    static int labelCount = 0;
    public Symbol(String id, PrimitiveType type) {
        this.id = id;
        this.type = type;
        this.size = type.size;
    }

    public Symbol(String id, String strType) {
        this.id = id;
        type = PrimitiveType.strToPrimitiveType(strType);
        this.size = type.size;
    }

    public static TypeSymbol newTemp(){
        TypeSymbol symbol = new TypeSymbol("%t"+tempCount);
        tempCount++;
        return symbol;
    }

    public static void resetTemp(){
        tempCount = 0;
    }

    public static TypeSymbol newLabel(){
        TypeSymbol symbol = new TypeSymbol("L"+labelCount);
        labelCount++;
        return symbol;
    }

    public static TypeSymbol newLabel(String name){
        TypeSymbol symbol = new TypeSymbol(name+labelCount);
        labelCount++;
        return symbol;
    }
    
    public String getName() {
        return id;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return id;
    }

    

}
