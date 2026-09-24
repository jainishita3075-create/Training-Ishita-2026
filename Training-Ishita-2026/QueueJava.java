import java.util.*;

public class QueueJava{

    static int front = -1;
    static int rear = -1;

    static void enqueue(int queue[], int value){
        if(rear == queue.length-1){
            System.out.println("Queue Overflow");
            return;
        }

        if(front == -1){
            front=0;
        }

        rear++;
        queue[rear] = value;

    }

    static int dequeue(int queue[]){
        if(front==rear){
            System.out.println("Queue Underflow");
            return -1;
        }
        int value = queue[front];
        front++;

        return value;
    }
    public static void main(String[] args){
        Scanner scn = new Scanner(System.in);
        System.out.println("Enter the number of Elements:");
        int n = scn.nextInt();
        
        int[] queue = new int[n];

        System.out.println("Enter the Elements:");
        for(int i=0; i<n; i++){
            int value = scn.nextInt();
            enqueue(queue, value);
        }

        System.out.println("Queue:" + Arrays.toString(queue));
        System.out.println("Removed Element:" + dequeue(queue));
    }
}