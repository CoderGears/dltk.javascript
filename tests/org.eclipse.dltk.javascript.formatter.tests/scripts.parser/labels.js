

function testBreak(x) {
   
	
	checkiandj:
		while (i < 4) {
		   document.write(i + "<br>");
		   i += 1;

		   checkj:
		   while (j > 4) {
		      document.write(j + "<br>");
		      j -= 1;
		      if ((j % 2) == 0)
		         continue checkj;
		      document.write(j + " is odd.<br>");
		   }
		   document.write("i = " + i + "<br>");
		   document.write("j = " + j + "<br>");
		}
	
	var i = 0;

	finish:   

	
   while (i < 6) {
      if (i == 3) {
         break finish;
      }
      i += 1;
   }
   
   return i * x;
}