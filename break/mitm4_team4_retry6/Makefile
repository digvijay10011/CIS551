main:
	echo "0xAardvark"
	ps -ef | grep bank | grep -v grep | awk '{print$2}' | xargs kill
	ps -ef | grep mitm | grep -v grep | awk '{print$2}' | xargs kill
