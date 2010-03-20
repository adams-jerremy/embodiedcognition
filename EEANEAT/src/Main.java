import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main{
	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		int i = Integer.parseInt(br.readLine());
		while(i-->0) System.out.println(zeroes(Integer.parseInt(br.readLine())));
	}
	public static int zeroes(int i){
		int d = 1, ret = 0;
		while(i/d>0) ret+=i/(d*=5);
		return ret;
	}
}