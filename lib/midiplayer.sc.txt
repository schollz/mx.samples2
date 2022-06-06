(
s.waitForBoot({

	var notes, on, off;

	// create a new instrument
	m=MxSamplesInstrument(s,"/home/zns/Documents/mx.samples2/lib/claus",400);
	m.setParam("release",2);
	// connect to midi
	MIDIClient.init;
	MIDIIn.connectAll;

	on = MIDIFunc.noteOn({ |veloc, num, chan, src|
		[num,veloc].postln;
		m.noteOn(num,veloc);

	});

	off = MIDIFunc.noteOff({ |veloc, num, chan, src|
		m.noteOff(num);
	});

});
)

m.setParam("amp",2)