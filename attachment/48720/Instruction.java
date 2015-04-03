import javax.persistence.JoinColumn;


public class Instruction {

	private static final int something = 40;

	//-----------------------------------------------------------------------

	@JoinColumn(name="test",bla="hi")
	int testfield;

}
