def buildLog = new File( basedir, 'build.log' )
assert buildLog.getText().contains( 'violates the strict version-alignment rule!' )
