import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

public class GeneratorVisitor extends GJDepthFirst<Symbol,Boolean>  {

    SymbolTable table;
    PrintStream outputStream;

    public GeneratorVisitor(SymbolTable table, PrintStream outputStream) {
        this.table = table;
        this.outputStream = outputStream;
    }

    void vTableGenerate(ClassDeclSymbol classDeclSymbol, ClassDeclSymbol thisClass){
        if(classDeclSymbol == null){
            return;
        } else {
            vTableGenerate(classDeclSymbol.parentClass, thisClass);
            if(classDeclSymbol.methods.isEmpty() 
                || (classDeclSymbol.parentClass != null && classDeclSymbol.methods.size() == classDeclSymbol.parentClass.methods.size())){
                    return;
                }

            int i = 1;
            for(Symbol symbol: classDeclSymbol.methods.values()){
                FunctionSymbol methodSymbol = (FunctionSymbol)symbol;

                if(classDeclSymbol.parentClass != null && classDeclSymbol.parentClass.methods.containsKey(symbol.id)){
                    i++;
                    continue;
                }
                
                outputStream.printf("i8* bitcast (%s (i8*", methodSymbol.returnType.type);
                for(Symbol arg: methodSymbol.args.values()){
                    outputStream.printf(", %s", arg.type);
                }
                outputStream.printf(")* ");
                if(methodSymbol.overrides.contains(thisClass)){
                    outputStream.printf("@%s.%s", thisClass, methodSymbol);
                } else {
                    outputStream.printf("@%s.%s", classDeclSymbol, methodSymbol);
                }
                outputStream.printf(" to i8*)");
                
                if(i != classDeclSymbol.methods.size()){
                    outputStream.printf(", ");
                }
                i++;
            }

            if(classDeclSymbol != thisClass){
                if(thisClass.parentClass != classDeclSymbol || thisClass.methods.size() != classDeclSymbol.methods.size()){
                    outputStream.printf(", ");
                }
            }
        }
    }

    

    @Override
    public Symbol visit(Goal n, Boolean argu) throws Exception {
        for(Symbol symbol: table.peek().values()){
            ClassDeclSymbol classDeclSymbol = (ClassDeclSymbol)symbol;
            outputStream.printf("@.%s_vtable = global [%d x i8*] [", classDeclSymbol, classDeclSymbol.methods.size());
            vTableGenerate(classDeclSymbol, classDeclSymbol);
            outputStream.printf("]\n");
        }
        outputStream.println();
        outputStream.println("declare i8* @calloc(i32, i32)");
        outputStream.println("declare i32 @printf(i8*, ...)");
        outputStream.println("declare void @exit(i32)");
        outputStream.println("@_cint = constant [4 x i8] c\"%d\\0a\\00\"");
        outputStream.println("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
        outputStream.println();
        outputStream.println("define void @print_int(i32 %i) {\n" +
            "\t%_str = bitcast [4 x i8]* @_cint to i8*\n" +
            "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
            "\tret void\n" +
        "}");
        outputStream.println();
        outputStream.println("define void @throw_oob() {\n" +
            "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n"+
            "\tcall i32 (i8*, ...) @printf(i8* %_str)\n"+
            "\tcall void @exit(i32 1)\n"+
            "\tret void\n"+
        "}");

        outputStream.println();
        
        

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
    public Symbol visit(MainClass n, Boolean argu) throws Exception {
        outputStream.println("define i32 @main() {");
        Symbol.resetTemp();
        TypeSymbol entry = Symbol.newLabel();
        Symbol.setCurrentLabel(entry);
        outputStream.printf("  %s:\n", entry);

        table.enter();

        n.f1.accept(this, argu);
        n.f11.accept(this, argu);
        
        n.f14.accept(this, argu);
        outputStream.println();

        n.f15.accept(this, argu);
        
        table.exit();
        outputStream.println("\tret i32 0");
        outputStream.println();
        outputStream.printf("  OOB_LABEL:\n");
        outputStream.println("\tcall void @throw_oob()");
        outputStream.println("\tunreachable");
        outputStream.println("}");
        outputStream.println();
        return null;
    }

    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */

    @Override
    public Symbol visit(VarDeclaration n, Boolean argu) throws Exception {
        TypeSymbol type = (TypeSymbol)n.f0.accept(this, argu);
        String typeName = type.getTypeName();

        String name = ((TypeSymbol)n.f1.accept(this, argu)).getTypeName();
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
            typeName = type.type.getTypeName();
        }

        if(argu){
            symbol.thisSymbol = table.getThis();
        }

        if(table.insert(name, symbol) != null){
            throw new DuplicateDeclarationException(name);
        }

        if(!argu){
            outputStream.printf("\t%%_%s = alloca %s\n", name, typeName);
        }       

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
    public Symbol visit(AssignmentStatement n, Boolean argu) throws Exception {
        String name = ((TypeSymbol)n.f0.accept(this, argu)).getTypeName();
        Symbol symbol = table.lookupField(name);

        String expr = n.f2.accept(this, argu).id;
        String type =  symbol.type.getTypeName();

        if(symbol.thisSymbol != null){
            TypeSymbol objectTemp = Symbol.newTemp();
            TypeSymbol objCast = Symbol.newTemp();
            // System.out.println(symbol.thisSymbol.fieldOffset);
            int offset = PrimitiveType.IDENTIFIER.getSize() + symbol.thisSymbol.fieldOffset.get(name);
            outputStream.printf("\t%s = getelementptr i8, i8* %%this, i32 %d\n", objectTemp, offset);
            outputStream.printf("\t%s = bitcast i8* %s to %s*\n", objCast, objectTemp, type);
            name = objCast.toString();
        } else {
            name = "%_" + name;
        }

        outputStream.printf("\tstore %s %s, %s* %s\n", type, expr, type, name);
        outputStream.println();

        return null;
    }

    void checkOutOfBounds(TypeSymbol name, TypeSymbol index){
        TypeSymbol inBounds = Symbol.newTemp();
        TypeSymbol lengthPtr = Symbol.newTemp();
        TypeSymbol length = Symbol.newTemp();

        TypeSymbol inBoundsLabel = Symbol.newLabel();
        
        outputStream.printf("\t%s = getelementptr i32, i32* %s, i32 -1\n", lengthPtr, name);
        outputStream.printf("\t%s = load i32, i32* %s\n", length, lengthPtr);
        
        outputStream.printf("\t%s = icmp slt i32 %s, %s\n", inBounds, index, length);
        outputStream.printf("\tbr i1 %s, label %%%s, label %%OOB_LABEL\n", inBounds, inBoundsLabel);
        outputStream.println();
        outputStream.printf("  %s:\n", inBoundsLabel);
        Symbol.setCurrentLabel(inBoundsLabel);
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
    public Symbol visit(ArrayAssignmentStatement n, Boolean argu) throws Exception {
        String name = n.f0.accept(this, argu).getName();
        TypeSymbol index = (TypeSymbol)n.f2.accept(this, argu);
        TypeSymbol arrayPtr = Symbol.newTemp();

        Symbol symbol = table.lookupField(name);

        if(symbol.thisSymbol != null){
            TypeSymbol objectTemp = Symbol.newTemp();
            TypeSymbol objCast = Symbol.newTemp();
            // System.out.println(symbol.thisSymbol.fieldOffset);
            int offset = PrimitiveType.IDENTIFIER.getSize() + symbol.thisSymbol.fieldOffset.get(name);
            outputStream.printf("\t%s = getelementptr i8, i8* %%this, i32 %d\n", objectTemp, offset);
            outputStream.printf("\t%s = bitcast i8* %s to %s*\n", objCast, objectTemp, symbol.type);
            name = objCast.toString();
        } else {
            name = "%_" + name;
        }

        outputStream.printf("\t%s = load i32*, i32** %s\n", arrayPtr, name);
        checkOutOfBounds(arrayPtr, index);
        
        TypeSymbol array = Symbol.newTemp();
        TypeSymbol expr = (TypeSymbol)n.f5.accept(this, argu);


        outputStream.printf("\t%s = getelementptr i32, i32* %s, i32 %s\n", array, arrayPtr, index);
        outputStream.printf("\tstore i32 %s, i32* %s\n", expr, array);
        outputStream.println();
       
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
    public Symbol visit(IfStatement n, Boolean argu) throws Exception {
        TypeSymbol expression = (TypeSymbol)n.f2.accept(this, argu);

        TypeSymbol thenLabel = Symbol.newLabel();
        TypeSymbol elseLabel = Symbol.newLabel();
        TypeSymbol next = Symbol.newLabel();

        outputStream.printf("\tbr i1 %s, label %%%s, label %%%s\n", expression, thenLabel, elseLabel);

        outputStream.println();
        outputStream.printf("  %s:\n", thenLabel);
        Symbol.setCurrentLabel(thenLabel);
        n.f4.accept(this, argu);
        outputStream.printf("\tbr label %%%s\n", next);
        
        outputStream.println();
        outputStream.printf("  %s:\n", elseLabel);
        Symbol.setCurrentLabel(elseLabel);
        n.f6.accept(this, argu);
        outputStream.printf("\tbr label %%%s\n", next);

        outputStream.println();
        outputStream.printf("  %s:\n", next);
        Symbol.setCurrentLabel(next);
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
    public Symbol visit(WhileStatement n, Boolean argu) throws Exception {
        TypeSymbol condLabel = Symbol.newLabel();
        
        outputStream.printf("\tbr label %%%s\n", condLabel);
        outputStream.println();
        outputStream.printf("  %s:\n", condLabel);
        Symbol.setCurrentLabel(condLabel);
        TypeSymbol expression = (TypeSymbol)n.f2.accept(this, argu);
        
        TypeSymbol loopLabel = Symbol.newLabel();
        TypeSymbol next = Symbol.newLabel();
        outputStream.printf("\tbr i1 %s, label %%%s, label %%%s\n", expression, loopLabel, next);
        
        outputStream.println();
        outputStream.printf("  %s:\n", loopLabel);
        Symbol.setCurrentLabel(loopLabel);
        n.f4.accept(this, argu);
        outputStream.printf("\tbr label %%%s\n", condLabel);


        outputStream.println();
        outputStream.printf("  %s:\n", next);
        Symbol.setCurrentLabel(next);
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
    public Symbol visit(PrintStatement n, Boolean argu) throws Exception {
        TypeSymbol expression = (TypeSymbol)n.f2.accept(this, argu);

        outputStream.printf("\tcall void @print_int(i32 %s)\n", expression);
        outputStream.println();
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
    public Symbol visit(Expression n, Boolean argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    
    /**
     * Grammar production:
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public Symbol visit(AndExpression n, Boolean argu) throws Exception {
        
        TypeSymbol result = Symbol.newTemp();
        
        TypeSymbol clause1 = (TypeSymbol)n.f0.accept(this, argu);
        TypeSymbol clause1Label = Symbol.getCurrentLabel();
        
        TypeSymbol clause2Label = Symbol.newLabel();
        TypeSymbol nextLabel = Symbol.newLabel();
        outputStream.printf("\tbr i1 %s, label %%%s, label %%%s\n", clause1, clause2Label, nextLabel);
        
        outputStream.println();
        outputStream.printf("  %s:\n", clause2Label);
        Symbol.setCurrentLabel(clause2Label);
        TypeSymbol clause2 = (TypeSymbol)n.f2.accept(this, argu);
        TypeSymbol clause2ReturnLabel = Symbol.getCurrentLabel();
        if(clause2ReturnLabel.equals(nextLabel)){
            clause2ReturnLabel = clause2Label;
        }

        outputStream.printf("\tbr label %%%s\n", nextLabel);
        
        outputStream.println();
        outputStream.printf("  %s:\n", nextLabel);
        Symbol.setCurrentLabel(nextLabel);
        outputStream.printf("\t%s = phi i1 [false, %%%s], [%s, %%%s]\n", result, clause1Label, clause2, clause2ReturnLabel);
        outputStream.println();

        return result;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */

    @Override
    public Symbol visit(CompareExpression n, Boolean argu) throws Exception {
        TypeSymbol expr1 = (TypeSymbol)n.f0.accept(this, argu);
        TypeSymbol expr2 = (TypeSymbol)n.f2.accept(this, argu);

        TypeSymbol temp = Symbol.newTemp();

        outputStream.printf("\t%s = icmp slt i32 %s, %s\n", temp, expr1, expr2);

        return temp;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */

    @Override
    public Symbol visit(PlusExpression n, Boolean argu) throws Exception {
        TypeSymbol expr1 = (TypeSymbol)n.f0.accept(this, argu);
        TypeSymbol expr2 = (TypeSymbol)n.f2.accept(this, argu);

        TypeSymbol temp = Symbol.newTemp();

        outputStream.printf("\t%s = add i32 %s, %s\n", temp, expr1, expr2);


        return temp;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */

    @Override
    public Symbol visit(MinusExpression n, Boolean argu) throws Exception {
        TypeSymbol expr1 = (TypeSymbol)n.f0.accept(this, argu);
        TypeSymbol expr2 = (TypeSymbol)n.f2.accept(this, argu);

        TypeSymbol temp = Symbol.newTemp();

        outputStream.printf("\t%s = sub i32 %s, %s\n", temp, expr1, expr2);


        return temp;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */

    @Override
    public Symbol visit(TimesExpression n, Boolean argu) throws Exception {
        TypeSymbol expr1 = (TypeSymbol)n.f0.accept(this, argu);
        TypeSymbol expr2 = (TypeSymbol)n.f2.accept(this, argu);

        TypeSymbol temp = Symbol.newTemp();

        outputStream.printf("\t%s = mul i32 %s, %s\n", temp, expr1, expr2);


        return temp;
    }

        /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */

    @Override
    public Symbol visit(ArrayLookup n, Boolean argu) throws Exception {
        TypeSymbol expr1 = (TypeSymbol)n.f0.accept(this, argu);
        TypeSymbol expr2 = (TypeSymbol)n.f2.accept(this, argu);

        checkOutOfBounds(expr1, expr2);

        TypeSymbol temp = Symbol.newTemp();
        TypeSymbol value = Symbol.newTemp();

        outputStream.printf("\t%s = getelementptr i32, i32* %s, i32 %s\n", temp, expr1, expr2);
        outputStream.printf("\t%s = load i32, i32* %s\n", value, temp);
        outputStream.println();
        return value;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */

    @Override
    public Symbol visit(ArrayLength n, Boolean argu) throws Exception {
        TypeSymbol expr1 = (TypeSymbol)n.f0.accept(this, argu);
        TypeSymbol temp = Symbol.newTemp();
        TypeSymbol value = Symbol.newTemp();

        outputStream.printf("\t%s = getelementptr i32, i32* %s, i32 -1\n", temp, expr1);
        outputStream.printf("\t%s = load i32, i32* %s\n", value, temp);
        outputStream.println();

        return value;
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
    public Symbol visit(MessageSend n, Boolean argu) throws Exception {
        Symbol expression = n.f0.accept(this, argu);

        String methodName = ((TypeSymbol)n.f2.accept(this, argu)).getTypeName();
        ClassDeclSymbol type;
        FunctionSymbol methodSymbol;
        
        // System.out.println(expression + " " + expression.getClass());
        if(expression.id.equals("%this")){
            type = table.getThis();
        } else {
            type = table.lookupType(((ClassSymbol)expression).className);
        }

        methodSymbol = (FunctionSymbol)type.methods.get(methodName);
        List<Symbol> args;
        List<Symbol> methodArgs = new ArrayList<>(methodSymbol.args.values());
        table.enter();

        n.f4.accept(this, argu);

        args = new ArrayList<>(table.exit().values());

        TypeSymbol temp = Symbol.newTemp();
        TypeSymbol v_table = Symbol.newTemp();
        TypeSymbol methodPtr = Symbol.newTemp();
        TypeSymbol method = Symbol.newTemp();
        TypeSymbol methodCast = Symbol.newTemp();
        TypeSymbol ret = Symbol.newTemp();

        outputStream.printf("\t%s = bitcast i8* %s to i8***\n", temp, expression.id);
        outputStream.printf("\t%s = load i8**, i8*** %s\n", v_table, temp);
        outputStream.printf("\t%s = getelementptr i8*, i8** %s, i32 %d\n", methodPtr, v_table, type.methodOffset.get(methodName));
        outputStream.printf("\t%s = load i8*, i8** %s\n", method, methodPtr);
        outputStream.printf("\t%s = bitcast i8* %s to %s (i8*", methodCast, method, methodSymbol.returnType.type.typeName);


        for(int i=0;i<methodArgs.size();i++){
            outputStream.printf(", %s", methodArgs.get(i).type.typeName);
        }
        outputStream.printf(")*\n");

        outputStream.printf("\t%s = call %s %s(i8* %s",ret, methodSymbol.returnType.type, methodCast, expression);
        for(int i=0;i<methodArgs.size();i++){
            outputStream.printf(", %s %s", methodArgs.get(i).type.typeName, args.get(i).getName());
        }
        outputStream.printf(")\n");

        outputStream.println();
        if(methodSymbol.returnType.type == PrimitiveType.IDENTIFIER){
            return new ClassSymbol(ret.id, methodSymbol.returnType.getName());
        } else {
            return ret;
        }
    }

    /**
     * Grammar production:
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */

    @Override
    public Symbol visit(ExpressionList n, Boolean argu) throws Exception {
        Symbol arg;
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
    public Symbol visit(ExpressionTail n, Boolean argu) throws Exception {
        int i = 1;
        Symbol arg;
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
    public Symbol visit(ExpressionTerm n, Boolean argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * Grammar production:
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */

    @Override
    public Symbol visit(Clause n, Boolean argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * Grammar production:
     * f0 -> "!"
     * f1 -> Clause()
     */

    @Override
    public Symbol visit(NotExpression n, Boolean argu) throws Exception {
        TypeSymbol expr1 = (TypeSymbol)n.f1.accept(this, argu);

        TypeSymbol temp = Symbol.newTemp();

        outputStream.printf("\t%s = xor i1 %s, true\n", temp, expr1);


        return temp;
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
    public Symbol visit(PrimaryExpression n, Boolean argu) throws Exception {
        
        Symbol type = n.f0.accept(this, argu);
        String name = type.id;
        Symbol symbol;
        // System.out.println(type);
        if(type.type == PrimitiveType.IDENTIFIER){
            symbol = table.lookupField(name);
            if(symbol == null){
                return type;
            }

            String strType = symbol.type.getTypeName();
            
            if(symbol.thisSymbol != null){
                TypeSymbol objectTemp = Symbol.newTemp();
                TypeSymbol objCast = Symbol.newTemp();
                int offset = PrimitiveType.IDENTIFIER.getSize() + symbol.thisSymbol.fieldOffset.get(name);
                outputStream.printf("\t%s = getelementptr i8, i8* %%this, i32 %d\n", objectTemp, offset);
                outputStream.printf("\t%s = bitcast i8* %s to %s*\n", objCast, objectTemp, strType);
                name = objCast.toString();
            } else {
                name = "%_" + name;
            }
            TypeSymbol temp = Symbol.newTemp();
            

            outputStream.printf("\t%s = load %s, %s* %s\n", temp, strType, strType, name);


            if(symbol.type != PrimitiveType.IDENTIFIER){
                type = temp;
            } else {
                type = new ClassSymbol(temp.id,((ClassSymbol)symbol).className);

            }

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
	public Symbol visit(ArrayAllocationExpression n, Boolean argu) throws Exception {

        TypeSymbol expr1 = (TypeSymbol)n.f3.accept(this, argu);

        TypeSymbol tempSize = Symbol.newTemp();
        TypeSymbol arrTemp = Symbol.newTemp();
        TypeSymbol arrCast = Symbol.newTemp();
        TypeSymbol array = Symbol.newTemp();

        outputStream.printf("\t%s = add i32 %s, 1\n", tempSize, expr1);

        outputStream.printf("\t%s = call i8* @calloc(i32 %s, i32 %d)\n", arrTemp, tempSize, PrimitiveType.INT.getSize());
        outputStream.printf("\t%s = bitcast i8* %s to i32*\n", arrCast, arrTemp);
        outputStream.printf("\tstore i32 %s, i32* %s\n", expr1, arrCast);
        outputStream.printf("\t%s = getelementptr i32, i32* %s, i32 1\n", array, arrCast);

		return array;
	}

    /**
     * Grammar production:
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */

    @Override
	public Symbol visit(AllocationExpression n, Boolean argu) throws Exception {
        TypeSymbol type = (TypeSymbol)n.f1.accept(this, argu);
        String typeName = type.getTypeName();

        ClassDeclSymbol classDeclSymbol = table.lookupType(typeName);
        TypeSymbol temp = Symbol.newTemp();
        TypeSymbol cast = Symbol.newTemp();
        TypeSymbol v_table = Symbol.newTemp();
        int size = PrimitiveType.IDENTIFIER.getSize() + classDeclSymbol.size;

        outputStream.printf("\t%s = call i8* @calloc(i32 1, i32 %d)\n", temp, size);
        outputStream.printf("\t%s = bitcast i8* %s to i8***\n",cast, temp);
        outputStream.printf("\t%s = getelementptr [%d x i8*], [%d x i8*]* @.%s_vtable, i32 0, i32 0\n", v_table, classDeclSymbol.methods.size(), classDeclSymbol.methods.size(), classDeclSymbol.id);
        outputStream.printf("\tstore i8** %s, i8*** %s\n", v_table, cast);
        
		return new ClassSymbol(temp.id, classDeclSymbol);

	}

    /**
     * Grammar production:
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
	public Symbol visit(BracketExpression n, Boolean argu) throws Exception {
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
    public Symbol visit(ClassDeclaration n, Boolean argu) throws Exception {
        String name = ((TypeSymbol)n.f1.accept(this, argu)).getTypeName();
        ClassDeclSymbol symbol = table.lookupType(name);
        table.insertThis(symbol);
        table.enter();

        n.f3.accept(this, true);

        table.enter(symbol.methods);

        n.f4.accept(this, argu);

        table.exit();
        table.exit();

        
        return null;

    }

    private void parentEnterHelper(ClassDeclSymbol parent, SymbolTable table){
        for(Symbol s: parent.fields.values()){
            s.thisSymbol = table.getThis();
        }
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
    public Symbol visit(ClassExtendsDeclaration n, Boolean argu) throws Exception {
        String className = ((TypeSymbol)n.f1.accept(this, argu)).getTypeName(); 
        String parentName = ((TypeSymbol)n.f3.accept(this, argu)).getTypeName();

        ClassDeclSymbol parent = table.lookupType(parentName);

        ClassDeclSymbol symbol = table.lookupType(className);

        table.insertThis(symbol);
        
        if(parent != null){
            parentEnterHelper(parent, table);
        }
        // table.print();


        table.enter();

        n.f5.accept(this, true);

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
    public Symbol visit(MethodDeclaration n, Boolean argu) throws Exception {
        TypeSymbol returnType = (TypeSymbol)n.f1.accept(this, argu);
        String returnTypeName = returnType.type.typeName;
        Map<String, Symbol> args;
  
        TypeSymbol method =  (TypeSymbol)n.f2.accept(this, argu);
        Symbol.resetTemp();

        outputStream.printf("define %s @%s.%s(i8* %%this", returnTypeName, table.getThis(), method);
        table.enter();

        n.f4.accept(this, argu);
        args = table.peek();
        outputStream.printf(") {\n");
        TypeSymbol entry = Symbol.newLabel();
        outputStream.printf("  %s:\n", entry);
        Symbol.setCurrentLabel(entry);
        for(Symbol symbol: args.values()){
            outputStream.printf("\t%%_%s = alloca %s\n", symbol, symbol.type);
            outputStream.printf("\tstore %s %%._%s, %s* %%_%s\n", symbol.type, symbol, symbol.type, symbol);
            
        }


        n.f7.accept(this, argu);
        outputStream.println();

        n.f8.accept(this, argu);

        Symbol expressionType = n.f10.accept(this, argu);

        outputStream.printf("\tret %s %s\n",returnType.type, expressionType);
        outputStream.println();
        outputStream.printf("  OOB_LABEL:\n");
        outputStream.println("\tcall void @throw_oob()");
        outputStream.println("\tunreachable");

        
        table.exit();

        outputStream.println("}");
        outputStream.println();

        return null;
    }


    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public Symbol visit(FormalParameter n, Boolean argu) throws Exception {
        TypeSymbol type = (TypeSymbol)n.f0.accept(this, argu);

        String name = ((TypeSymbol)n.f1.accept(this, argu)).getTypeName();
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
        }

        outputStream.printf(", %s %%._%s", type.type.getTypeName(), name);

        return null;
    }
    
    @Override
	public Symbol visit(IntegerLiteral n, Boolean argu) throws Exception {
		return new TypeSymbol(n.f0.toString(), PrimitiveType.INT);
	}

	@Override
	public Symbol visit(TrueLiteral n, Boolean argu) throws Exception {
		return new TypeSymbol("true", PrimitiveType.BOOLEAN);
	}

	@Override
	public Symbol visit(FalseLiteral n, Boolean argu) throws Exception {
		return new TypeSymbol("false", PrimitiveType.BOOLEAN);
	}

	@Override
	public Symbol visit(ThisExpression n, Boolean argu) throws Exception {
        return new TypeSymbol("%" + n.f0.toString());
	}

    @Override
    public Symbol visit(ArrayType n, Boolean argu) {
        return new TypeSymbol(PrimitiveType.ARRAY);
    }

    public Symbol visit(BooleanType n, Boolean argu) {
        return new TypeSymbol(PrimitiveType.BOOLEAN);
    }

    public Symbol visit(IntegerType n, Boolean argu) {
        return new TypeSymbol(PrimitiveType.INT);
    }

    @Override
    public Symbol visit(Identifier n, Boolean argu) {
        return new TypeSymbol(n.f0.toString());
    }

    
    
}
