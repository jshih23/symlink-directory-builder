package testing;

public class PathEditTest {

	public static void main(String[] args) {
		String p = "dogfood";
		
		System.out.println(cutPath(p));

	}
	
	public static String cutPath(String p){
		if (p.startsWith("dog")){
			return p.substring("dog".length());
		}
		return p;
	}

}
