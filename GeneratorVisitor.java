import java.util.Map;

import javax.swing.text.Style;

import syntaxtree.AllocationExpression;
import syntaxtree.AndExpression;
import syntaxtree.ArrayAllocationExpression;
import syntaxtree.ArrayAssignmentStatement;
import syntaxtree.ArrayLength;
import syntaxtree.ArrayLookup;
import syntaxtree.ArrayType;
import syntaxtree.AssignmentStatement;
import syntaxtree.BooleanType;
import syntaxtree.BracketExpression;
import syntaxtree.ClassDeclaration;
import syntaxtree.ClassExtendsDeclaration;
import syntaxtree.Clause;
import syntaxtree.CompareExpression;
import syntaxtree.Expression;
import syntaxtree.ExpressionList;
import syntaxtree.ExpressionTail;
import syntaxtree.ExpressionTerm;
import syntaxtree.FalseLiteral;
import syntaxtree.FormalParameter;
import syntaxtree.Goal;
import syntaxtree.Identifier;
import syntaxtree.IfStatement;
import syntaxtree.IntegerLiteral;
import syntaxtree.IntegerType;
import syntaxtree.MainClass;
import syntaxtree.MessageSend;
import syntaxtree.MethodDeclaration;
import syntaxtree.MinusExpression;
import syntaxtree.Node;
import syntaxtree.NotExpression;
import syntaxtree.PlusExpression;
import syntaxtree.PrimaryExpression;
import syntaxtree.PrintStatement;
import syntaxtree.ThisExpression;
import syntaxtree.TimesExpression;
import syntaxtree.TrueLiteral;
import syntaxtree.VarDeclaration;
import syntaxtree.WhileStatement;
import visitor.GJDepthFirst;

public class GeneratorVisitor extends GJDepthFirst<TypeSymbol,Boolean>  {

    SymbolTable table;

    public GeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    

    @Override
    public TypeSymbol visit(Goal n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        System.out.println("declare i8* @calloc(i32, i32)");
        System.out.println("declare i32 @printf(i8*, ...)");
        System.out.println("declare void @exit(i32)");
        System.out.println("@_cint = constant [4 x i8] c\"%d\\0a\\00\"");
        System.out.println("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
        System.out.println();
        System.out.println("define void @print_int(i32 %i) {\n" +
            "\t%_str = bitcast [4 x i8]* @_cint to i8*\n" +
            "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
            "\tret void\n" +
        "}");

        System.out.println("define void @throw_oob() {\n" +
            "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n"+
            "\tcall i32 (i8*, ...) @printf(i8* %_str)\n"+
            "\tcall void @exit(i32 1)\n"+
            "\tret void\n"+
        "}");
        
        

        return super.visit(n, argu);
    }



    /**
     * Grammar production:
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */

    @Override
    public TypeSymbol visit(MainClass n, Boolean argu) throws Exception {
        System.out.println("define i32 @main() {");

        table.enter();
        super.visit(n, argu);
        table.exit();

        System.out.println("\tret i32 0");
        System.out.println("}");
        return null;
    }

    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */

    @Override
    public TypeSymbol visit(VarDeclaration n, Boolean argu) throws Exception {
        TypeSymbol type = n.f0.accept(this, argu);
        String typeName = type.getTypeName();

        String name = n.f1.accept(this, argu).getTypeName();
        Symbol symbol;

        if(type.type != PrimitiveType.IDENTIFIER){
            symbol = new Symbol(name, type.type);
        } else {
            ClassDeclSymbol classSym = (ClassDeclSymbol)table.lookupType(typeName);
            if(classSym != null){
                symbol = new ClassSymbol(name, classSym);
            } else {
                throw new Exception("Type " + typeName + " not defined");
            }
        }
        if(table.insert(name, symbol) != null){
            throw new DuplicateDeclarationException(name);
        }


        System.out.printf("\t%s = alloca %s\n", name, typeName);
        

        return null;
    }


    /**
     * Grammar production:
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public TypeSymbol visit(AssignmentStatement n, Boolean argu) throws Exception {
        String name = n.f0.accept(this, argu).getTypeName();

        Symbol symbol = table.lookupField(name);

        String expr = n.f2.accept(this, argu).toString();

        String type =  symbol.type.getTypeName();

        System.out.printf("\tstore %s %s, %s* %s\n", type, expr, type, name);

        return null;
    }

    /**
     * Grammar production:
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */

    @Override
    public TypeSymbol visit(ArrayAssignmentStatement n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub

        String name = n.f0.accept(this, argu).getTypeName();

        Symbol symbol = table.lookupField(name);

        if(symbol == null){
            throw new DeclarationException(name);
            // throw new Exception("Next time, do us the favor and declare the variable " + name);
        }

        if(symbol.type != PrimitiveType.ARRAY){
            throw new TypeException(PrimitiveType.ARRAY.typeName, symbol.type.getTypeName());
            // throw new Exception("Type must be array. Was " + symbol.type.getTypeName());
        }

        TypeSymbol index = n.f2.accept(this, argu);

        if(index.type != PrimitiveType.INT){
            throw new Exception("Array index must be an integer");
        }

        TypeSymbol expr = n.f5.accept(this, argu);

        if(expr.type != PrimitiveType.INT){
            throw new TypeException(PrimitiveType.INT.typeName, expr.getTypeName());
            // throw new Exception("Type must be integer");
        }

        return null;
    }

    /**
     * Grammar production:
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */

    @Override
    public TypeSymbol visit(IfStatement n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub

        TypeSymbol expression = n.f2.accept(this, argu);

        if(expression.type != PrimitiveType.BOOLEAN){
            throw new TypeException(PrimitiveType.BOOLEAN.typeName, expression.getTypeName());
            // throw new Exception("Only boolean expression allowed");
        }

        return null;
    }

    /**
     * Grammar production:
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */

    @Override
    public TypeSymbol visit(WhileStatement n, Boolean argu) throws Exception {
        TypeSymbol expression = n.f2.accept(this, argu);

        if(expression.type != PrimitiveType.BOOLEAN){
            throw new TypeException(PrimitiveType.BOOLEAN.typeName, expression.getTypeName());
            // throw new Exception("Only boolean expression allowed");
        }

        return null;
    }

    /**
     * Grammar production:
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */

    @Override
    public TypeSymbol visit(PrintStatement n, Boolean argu) throws Exception {
        TypeSymbol expression = n.f2.accept(this, argu);

        System.out.printf("\tcall void @print_int(i32 %s)\n", expression);
        return null;
    }

    /**
     * Grammar production:
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | Clause()
     */
    
    @Override
    public TypeSymbol visit(Expression n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        return n.f0.accept(this, argu);
    }

    
    /**
     * Grammar production:
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public TypeSymbol visit(AndExpression n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub

        TypeSymbol clause1 = n.f0.accept(this, argu);
        TypeSymbol clause2 = n.f2.accept(this, argu);

        if(clause1.type != PrimitiveType.BOOLEAN || clause2.type != PrimitiveType.BOOLEAN){
            throw new Exception("Types must be boolean");
        }

        return new TypeSymbol(PrimitiveType.BOOLEAN);
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */

    @Override
    public TypeSymbol visit(CompareExpression n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        TypeSymbol expr1 = n.f0.accept(this, argu);
        TypeSymbol expr2 = n.f2.accept(this, argu);

        if(expr1.type != PrimitiveType.INT || expr2.type != PrimitiveType.INT){
            throw new Exception("Types must be integers");
        }

        return new TypeSymbol(PrimitiveType.BOOLEAN);
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */

    @Override
    public TypeSymbol visit(PlusExpression n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        TypeSymbol expr1 = n.f0.accept(this, argu);
        TypeSymbol expr2 = n.f2.accept(this, argu);

        TypeSymbol temp = Symbol.newTemp();

        


        return temp;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */

    @Override
    public TypeSymbol visit(MinusExpression n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        TypeSymbol expr1 = n.f0.accept(this, argu);
        TypeSymbol expr2 = n.f2.accept(this, argu);

        if(expr1.type != PrimitiveType.INT || expr2.type != PrimitiveType.INT){
            throw new Exception("Types must be integers");
        }

        return new TypeSymbol(PrimitiveType.INT);
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */

    @Override
    public TypeSymbol visit(TimesExpression n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        TypeSymbol expr1 = n.f0.accept(this, argu);
        TypeSymbol expr2 = n.f2.accept(this, argu);

        if(expr1.type != PrimitiveType.INT || expr2.type != PrimitiveType.INT){
            throw new Exception("Types must be integers");
        }

        return new TypeSymbol(PrimitiveType.INT);
    }

        /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */

    @Override
    public TypeSymbol visit(ArrayLookup n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        TypeSymbol expr1 = n.f0.accept(this, argu);
        TypeSymbol expr2 = n.f2.accept(this, argu);

        if(expr1.type != PrimitiveType.ARRAY){
            throw new TypeException(PrimitiveType.ARRAY.typeName, expr1.getTypeName());
            // throw new Exception("Type must be array");
        }

        if(expr2.type != PrimitiveType.INT){
            throw new Exception("Array index must be an integer");
        }

        return new TypeSymbol(PrimitiveType.INT);
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */

    @Override
    public TypeSymbol visit(ArrayLength n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        TypeSymbol expr1 = n.f0.accept(this, argu);

        if(expr1.type != PrimitiveType.ARRAY){
            throw new TypeException(PrimitiveType.ARRAY.typeName, expr1.getTypeName());
            // throw new Exception("Type must be array");
        }

        return new TypeSymbol(PrimitiveType.INT);
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */

    @Override
    public TypeSymbol visit(MessageSend n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub

        TypeSymbol expression = n.f0.accept(this, argu);

        if(expression.type != PrimitiveType.IDENTIFIER){
            throw new TypeException("object type or this", expression.getTypeName());
            // throw new Exception("Expected object type");
        }

        FunctionSymbol method;
        String methodName = n.f2.accept(this, argu).getTypeName();

        if(expression.getTypeName().equals("this")){
            method = table.lookupMethod(methodName);

            if(method == null){
                throw new Exception("Method " + methodName + " not defined");
            }
        } else {
            ClassDeclSymbol classSymbol = (ClassDeclSymbol)table.lookup(expression.getTypeName());

            method = (FunctionSymbol)classSymbol.methods.get(methodName);

            if(method == null){
                throw new Exception("Method " + methodName + " not defined in class " + classSymbol.id);
            }

        }
        Map<String, Symbol> args;
        table.enter();

        n.f4.accept(this, argu);

        args = table.exit();

        method.checkArgs(args, table);

        return method.returnType;
    }

    /**
     * Grammar production:
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */

    @Override
    public TypeSymbol visit(ExpressionList n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        TypeSymbol arg;
        arg = n.f0.accept(this, argu);
        table.insert("arg0", arg);
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * Grammar production:
     * f0 -> ( ExpressionTerm() )*
     */

    @Override
    public TypeSymbol visit(ExpressionTail n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        int i = 1;
        TypeSymbol arg;
        for(Node node: n.f0.nodes){
            arg = node.accept(this, argu);
            table.insert("arg" + i, arg);
            i++;
        }

        return null;
    }


    /**
     * Grammar production:
     * f0 -> ","
     * f1 -> Expression()
     */

    @Override
    public TypeSymbol visit(ExpressionTerm n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        return n.f1.accept(this, argu);
    }

    /**
     * Grammar production:
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */

    @Override
    public TypeSymbol visit(Clause n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        return n.f0.accept(this, argu);
    }

    /**
     * Grammar production:
     * f0 -> "!"
     * f1 -> Clause()
     */

    @Override
    public TypeSymbol visit(NotExpression n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        TypeSymbol expr1 = n.f1.accept(this, argu);

        if(expr1.type != PrimitiveType.BOOLEAN){
            throw new TypeException(PrimitiveType.BOOLEAN.typeName, expr1.getTypeName());
            // throw new Exception("Type must be boolean");
        }

        return new TypeSymbol(PrimitiveType.BOOLEAN);
    }

    /**
     * Grammar production:
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */

    @Override
    public TypeSymbol visit(PrimaryExpression n, Boolean argu) throws Exception {
        
        TypeSymbol type = n.f0.accept(this, argu);
        String name = type.getTypeName();
        Symbol symbol;


        // System.out.println("TypeSymbol " + type + " "+ name);
        if(name.equals("this")){
            return new TypeSymbol(table.getThis().id);
        }

        if(type.type == PrimitiveType.IDENTIFIER){
            symbol = table.lookup(name);
            if(symbol.type != PrimitiveType.IDENTIFIER){

                String strType = symbol.type.getTypeName();

                Symbol temp = Symbol.newTemp();

                System.out.printf("\t%s = load %s, %s* %s\n", temp, strType, strType, name);

                type = (TypeSymbol)temp;
            } 
            // else {
            //     if(symbol instanceof ClassSymbol){
            //         type = new TypeSymbol(((ClassSymbol)symbol).className);
            //     } else if(symbol instanceof ClassDeclSymbol){
            //         type = new TypeSymbol(((ClassDeclSymbol)symbol).id);
            //     }

            // }
        }



        return type;
    }
    
    /**
     * Grammar production:
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */

    @Override
	public TypeSymbol visit(ArrayAllocationExpression n, Boolean argu) throws Exception {
		// TODO Auto-generated method stub

        TypeSymbol expr1 = n.f3.accept(this, argu);

        if(expr1.type != PrimitiveType.INT){
            throw new Exception("Array index must be an integer");
        }

		return new TypeSymbol(PrimitiveType.ARRAY);
	}

    /**
     * Grammar production:
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */

    @Override
	public TypeSymbol visit(AllocationExpression n, Boolean argu) throws Exception {
		// TODO Auto-generated method stub
        TypeSymbol type = n.f1.accept(this, argu);
        String typeName = type.getTypeName();

        if(type.type != PrimitiveType.IDENTIFIER){
            throw new Exception("Cannot instantiate " + typeName);
        } else if(table.lookupType(typeName) == null){
            throw new Exception("Type " + typeName + " not defined");
        }
		return type;
	}

    /**
     * Grammar production:
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
	public TypeSymbol visit(BracketExpression n, Boolean argu) throws Exception {
		// TODO Auto-generated method stub
		return n.f1.accept(this, argu);
	}

	/**
     * Grammar production:
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    
    @Override
    public TypeSymbol visit(ClassDeclaration n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        String name = n.f1.accept(this, argu).getTypeName();
        ClassDeclSymbol symbol = table.lookupType(name);
        table.insertThis(symbol);
        table.enter();

        n.f3.accept(this, argu);

        for(Symbol s: symbol.methods.values()){
            if(table.lookupType(s.id) != null){
                throw new Exception("Cannot have method with name of class");
            }
        }

        table.enter(symbol.methods);

        n.f4.accept(this, argu);

        table.exit();
        table.exit();

        
        return null;

    }

    private void parentEnterHelper(ClassDeclSymbol parent, SymbolTable table){
        if(parent.parentClass == null){
            table.enter(parent.fields);
        } else {
            parentEnterHelper(parent.parentClass, table);
            table.enter(parent.fields);
        }
    }

    private void parentExitHelper(ClassDeclSymbol parent, SymbolTable table){
        if(parent.parentClass == null){
            table.exit();
        } else {
            parentExitHelper(parent.parentClass, table);
            table.exit();
        }
    }

    /**
     * Grammar production:
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */

    @Override
    public TypeSymbol visit(ClassExtendsDeclaration n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        String className = n.f1.accept(this, argu).getTypeName(); 
        String parentName = n.f3.accept(this, argu).getTypeName();

        ClassDeclSymbol parent = table.lookupType(parentName);

        if(parent == null){
            throw new Exception("Type " + parentName + " not declared in file");
        }

        ClassDeclSymbol symbol = table.lookupType(className);

        table.insertThis(symbol);
        
        if(parent != null){
            parentEnterHelper(parent, table);
        }
        // table.print();


        table.enter();

        n.f5.accept(this, argu);

        table.enter(symbol.methods);

        n.f6.accept(this, argu);
        
        table.exit();
        table.exit();
        
        parentExitHelper(parent, table);
        
        return null;
    }

        /**
     * Grammar production:
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */

    @Override
    public TypeSymbol visit(MethodDeclaration n, Boolean argu) throws Exception {
        TypeSymbol returnType = n.f1.accept(this, argu);
        String returnTypeName = returnType.getTypeName();
        ClassDeclSymbol classSym = null;
        if(returnType.type == PrimitiveType.IDENTIFIER){
            classSym = (ClassDeclSymbol)table.lookupType(returnTypeName);
            if(classSym == null){
                throw new Exception("Type " + returnTypeName + " not defined");
            }
        }


        n.f2.accept(this, argu).getTypeName();
        table.enter();

        n.f4.accept(this, argu);
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        

        TypeSymbol expressionType = n.f10.accept(this, argu);

        if(expressionType.type != returnType.type){
            throw new Exception("Return type mismatch. Expected: " + returnType.getTypeName() + " but got: " + expressionType.getTypeName());
        } else if(expressionType.type == PrimitiveType.IDENTIFIER){
            ClassDeclSymbol exprClass = table.lookupType(expressionType.getTypeName());
            ClassSymbol exprSymbol;
            if(exprClass == null){
                throw new Exception("Type " + expressionType.getTypeName() + " not defined");
            } else {
                exprSymbol = new ClassSymbol("return", exprClass);
            }
            
            if(!exprSymbol.isInstanceOf(classSym)) {
                throw new Exception("Type " + exprSymbol.className + " not instance of " + classSym.id);
            }
            
        }

        
        table.exit();

        return null;
    }


    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public TypeSymbol visit(FormalParameter n, Boolean argu) throws Exception {
        // TODO Auto-generated method stub
        // System.out.println("Parameter");
        TypeSymbol type = n.f0.accept(this, argu);

        String name = n.f1.accept(this, argu).getTypeName();;
        String typeName = type.getTypeName();
        Symbol symbol;

        if(type.type != PrimitiveType.IDENTIFIER){
            symbol = new Symbol(name, type.type);
        } else {
            ClassDeclSymbol classSym = (ClassDeclSymbol)table.lookupType(typeName);
            if(classSym != null){
                symbol = new ClassSymbol(name, classSym);
            } else {
                throw new Exception("Type " + typeName + " not defined");
            }
        }
        if(table.insert(name, symbol) != null){
            throw new DuplicateDeclarationException(name);
            // throw new Exception("Duplicate use of name " + name);
        }


        return null;
    }
    
    @Override
	public TypeSymbol visit(IntegerLiteral n, Boolean argu) throws Exception {
		// TODO Auto-generated method stub
		return new TypeSymbol(n.f0.toString(), PrimitiveType.INT);
	}

	@Override
	public TypeSymbol visit(TrueLiteral n, Boolean argu) throws Exception {
		// TODO Auto-generated method stub
		return new TypeSymbol("true", PrimitiveType.BOOLEAN);
	}

	@Override
	public TypeSymbol visit(FalseLiteral n, Boolean argu) throws Exception {
		// TODO Auto-generated method stub
		return new TypeSymbol("false", PrimitiveType.BOOLEAN);
	}

	@Override
	public TypeSymbol visit(ThisExpression n, Boolean argu) throws Exception {
		// TODO Auto-generated method stub
        return new TypeSymbol(n.f0.toString());
	}

    @Override
    public TypeSymbol visit(ArrayType n, Boolean argu) {
        return new TypeSymbol(PrimitiveType.ARRAY);
    }

    public TypeSymbol visit(BooleanType n, Boolean argu) {
        return new TypeSymbol(PrimitiveType.BOOLEAN);
    }

    public TypeSymbol visit(IntegerType n, Boolean argu) {
        return new TypeSymbol(PrimitiveType.INT);
    }

    @Override
    public TypeSymbol visit(Identifier n, Boolean argu) {
        return new TypeSymbol("%" + n.f0.toString());
    }

    
    
}
