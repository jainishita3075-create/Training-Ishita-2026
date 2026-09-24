import java.util.*;

public class StackJava{

    static int top = -1;

    static void push(int[] stack, int value){
        if(top == stack.length-1){
            System.out.println("Stack Overflow");
            return;
        }else{
            top++;
            stack[top]=value;
        }
    }

    static int pop(int[] stack){
        if(top == -1){
            System.out.println("Stack Underflow");
            return -1;
        }else{
            int value = stack[top];
            top--;

            return value;
        }
    }

    static int peek(int[] stack){
        if(top == -1){
            System.out.println("Stack is Empty");
            return -1;
        }

         return stack[top];
    }

    public static void main(String[] args){
        Scanner scn = new Scanner(System.in);
        System.out.println("Enter the number of elements:");
        int n = scn.nextInt();
        int stack[] = new int[n];

        System.out.println("Enter the Elements:");
       for(int i=0; i<n; i++){
            int value = scn.nextInt();
            push(stack,value);
       }
       System.out.println(Arrays.toString(stack));
       System.out.println("Top element:"+(peek(stack)));
       System.out.println("Removed Element:"+pop(stack));
    }
}