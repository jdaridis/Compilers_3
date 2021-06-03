import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import syntaxtree.Goal;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        for(int i = 0; i < args.length;i++){
            FileInputStream fis = null;
            try{
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);

                String filename = args[i].split(".java")[0];
                System.out.println(filename);

                PrintStream outPrintStream = new PrintStream(filename+".ll");

                Goal root = parser.Goal();

                DeclarationsVisitor declarations = new DeclarationsVisitor();
                SymbolTable symbolTable = new SymbolTable();

                root.accept(declarations, symbolTable);
                // symbolTable.print();

                TypesVisitor typeCheck = new TypesVisitor();

                root.accept(typeCheck, symbolTable);

                for(Symbol s: symbolTable.peek().values()){
                    ClassDeclSymbol classSym = (ClassDeclSymbol)s;
                    int fieldOffset = 0; // computeClassSize(classSym.parentClass, symbolTable);
                    int methodOffset = 0;
                    
                    for(Symbol field: classSym.fields.values()){
                        classSym.fieldOffset.put(field.id, fieldOffset);
                        fieldOffset += field.type.getSize();
                    }

                    
                    classSym.size = fieldOffset;
                    
                    for(Symbol method: classSym.methods.values()){
                        classSym.methodOffset.put(method.id, methodOffset);
                        methodOffset += method.type.getSize();
                    }
                }

                GeneratorVisitor generator = new GeneratorVisitor(symbolTable, outPrintStream);

                root.accept(generator, false);

                symbolTable.exit();
                
            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
        }

        
    }


    public static int computeClassSize(ClassDeclSymbol symbol, SymbolTable table){
        int size;
        if(symbol != null && symbol.size != 0){
            return symbol.size;
        } else {
            if(symbol == null){
                return 0;
            } else {
                size = computeClassSize(symbol.parentClass, table);
                for(Symbol field: symbol.fields.values()){
                    size += field.type.getSize();
                }
                for(Symbol s: symbol.methods.values()){
                    size += symbol.size;
                }
                symbol.size = size;
                return size;
            }
        }
        
    }

}
