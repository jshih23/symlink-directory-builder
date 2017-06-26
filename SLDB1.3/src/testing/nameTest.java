package testing;

public class nameTest {

	public static void main(String[] args) {
		String keck_list = "/work/sirius/opkgbuild/limo_engine_pp1/src/obj_limo_engine_pp1_ram_arel/keck_file_list"; //getKeckChosen().getPath();
		
		String pathFromObj = keck_list.substring(keck_list.indexOf("obj_")+4);

		int slashIndex = pathFromObj.indexOf("/");

		String name = pathFromObj.substring(0, slashIndex);
		
	}

}
