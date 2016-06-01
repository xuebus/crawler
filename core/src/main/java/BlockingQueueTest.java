import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

 public class BlockingQueueTest {
     public static void main(String[] args) {
         final BlockingQueue queue = new ArrayBlockingQueue(3);
         for(int i=0;i<2;i++){
             new Thread(){
                 public void run(){
                     while(true){
                         try {
                             Thread.sleep((long)(100));
                             System.out.println(Thread.currentThread().getName() + "Prepare input data");                            
                             queue.put(Integer.valueOf(1)); 
                             System.out.println(Thread.currentThread().getName() + "Data have inputed into queue" +"The Queue have" + queue.size() + "  data  now!");
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }
 
                     }
                 }
                 
             }.start();
         }
         
         new Thread(){
             public void run(){
                 while(true){
                     try {
                    	 //将此处的睡眠时间分别改为100和1000，观察运行结果
                         Thread.sleep(50000);
                         System.out.println(Thread.currentThread().getName() + "Prepare get data!");
                         queue.take();
                         System.out.println(Thread.currentThread().getName() + "Have got out the data" +                             
                                 "The Queue have" + queue.size() + " data now");                    
                     } catch (InterruptedException e) {
                         e.printStackTrace();
                     }
                 }
             }
             
         }.start();            
     }
 }