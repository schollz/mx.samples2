MxSamplesInstrument {

	var server;

	var <folder;
	var maxSamples;

	var <noteNumbers;
	var <noteDynamics;
	var <noteRoundRobins;

	var <buf;
	var bufTime;
	var <syn;
	var <params;


	*new {
		arg serverName,folderToSamples,numberMaxSamples;
		^super.new.init(serverName,folderToSamples,numberMaxSamples);
	}

	init {
		arg serverName,folderToSamples,numberMaxSamples;

		server=serverName;
		folder=folderToSamples;
		maxSamples=numberMaxSamples;

		buf=Dictionary.new();
		syn=Dictionary.new();
		noteDynamics=Dictionary.new();
		noteRoundRobins=Dictionary.new();
		noteNumbers=Array.new(128);
		params = Dictionary.newFrom([
			"amp", 1.0,
			"pan", 0.0,
			"attack", 0.01,
			"decay", 0.1,
			"sustain", 1.0,
			"release", 1.0,
			"fadetime",1.0,
		]);


		PathName.new(folder).entries.do({ arg v;
			var fileSplit=v.fileName.split($.);
			var note,dyn,dyns,rr,rel;
			if (fileSplit.last=="wav",{
				if (fileSplit.size==6,{
					note=fileSplit[0].asInteger;
					dyn=fileSplit[1].asInteger;
					dyns=fileSplit[2].asInteger;
					rr=fileSplit[3].asInteger;
					rel=fileSplit[4].asInteger;
					if (noteDynamics.at(note).isNil,{
						noteDynamics.put(note,dyns);
						noteNumbers.add(note);
					});
					if (noteRoundRobins.at(note.asString++"."++dyn.asString).isNil,{
						noteRoundRobins.put(note.asString++"."++dyn.asString,rr);
					},{
						if (rr>noteRoundRobins.at(note.asString++"."++dyn.asString),{
							noteRoundRobins.put(note.asString++"."++dyn.asString,rr);
						});
					});
				});
			});
		});

		noteNumbers=noteNumbers.sort;


		SynthDef("playx2",{
			arg out=0,pan=0,amp=1.0,
			buf1,buf2,buf1mix=1,
			t_trig=1,rate=1,
			fade_trig=0,fade_time=0.1,
			attack=0.01,decay=0.1,sustain=1.0,release=1,gate=1,
			startPos=0,lpf=18000,
			busReverb,busDelay,sendReverb=0,sendDelay=0;
			var snd,snd2;
			var frames1=BufFrames.ir(buf1);
			var frames2=BufFrames.ir(buf2);
			rate=rate*BufRateScale.ir(buf1);
			snd=PlayBuf.ar(2,buf1,rate,t_trig,startPos:startPos*frames1,doneAction:Select.kr(frames1>frames2,[0,2]));
			snd2=PlayBuf.ar(2,buf2,rate,t_trig,startPos:startPos*frames2,doneAction:Select.kr(frames2>frames1,[0,2]));
			snd=SelectX.ar(buf1mix,[snd2,snd]);
			snd=snd*EnvGen.ar(Env.adsr(attack,decay,sustain,release),gate,doneAction:2);
			snd=snd*EnvGen.ar(Env.new([1,0],[fade_time]),fade_trig,doneAction:2);
			DetectSilence.ar(snd,0.00001,doneAction:2);
			snd=Balance2.ar(snd[0],snd[1],pan,amp);
			Out.ar(out,snd);
			Out.ar(busReverb,snd*sendReverb);
			Out.ar(busDelay,snd*sendDelay);
		}).send(server);

		SynthDef("playx1",{
			arg out=0,pan=0,amp=1.0,
			buf1,buf2,buf1mix=1,
			t_trig=1,rate=1,
			fade_trig=0,fade_time=0.1,
			attack=0.01,decay=0.1,sustain=1.0,release=1,gate=1,
			startPos=0,lpf=18000,
			busReverb,busDelay,sendReverb=0,sendDelay=0;
			var snd,snd2;
			var frames1=BufFrames.ir(buf1);
			var frames2=BufFrames.ir(buf2);
			rate=rate*BufRateScale.ir(buf1);
			snd=PlayBuf.ar(1,buf1,rate,t_trig,startPos:startPos*frames1,doneAction:Select.kr(frames1>frames2,[0,2]));
			snd2=PlayBuf.ar(1,buf2,rate,t_trig,startPos:startPos*frames2,doneAction:Select.kr(frames2>frames1,[0,2]));
			snd=SelectX.ar(buf1mix,[snd2,snd]);
			snd=snd*EnvGen.ar(Env.adsr(attack,decay,sustain,release),gate,doneAction:2);
			snd=snd*EnvGen.ar(Env.new([1,0],[fade_time]),fade_trig,doneAction:2);
			DetectSilence.ar(snd,0.00001,doneAction:2);
			snd=Pan2.ar(snd,pan,amp);
			Out.ar(out,snd);
			Out.ar(busReverb,snd*sendReverb);
			Out.ar(busDelay,snd*sendDelay);
		}).send(server);

	}

	garbageCollect {
		Routine {
			loop {
				1.wait;
				while ({buf.size>maxSamples},{
					var toRemove=buf.keys.asArray[0];
					("removing "++toRemove).postln;
					buf.put(toRemove,nil);
				});
			}
		}.play;
	}

	setParam {
		arg key,value;
		params.put(key,value);
	}

	noteOn {
		arg note,velocity;
		var noteOriginal=note;
		var noteClosest=noteNumbers[noteNumbers.indexIn(note)];
		var noteAbsDifference;
		var rate=1.0;
		var buf1mix=1.0;
		var amp=1.0;
		var file1,file2;
		var velIndex;
		var velIndices;
		var vels;
		var dyns;

		// first determine the rate to get the right note
		while ({note<noteClosest},{
			note=note+12;
			rate=rate*0.5;
		});

		while ({note-noteClosest>11},{
			note=note-12;
			rate=rate*2;
		});
		rate=rate*Scale.chromatic.ratios[note-noteClosest];

		// determine the number of dynamics
		dyns=noteDynamics.at(noteClosest);

		// determine file 1 and 2 interpolation
		file1=noteClosest.asInteger.asString++".";
		file2=noteClosest.asInteger.asString++".";
		if (noteDynamics[noteClosest]<2,{
			// simple playback using amp
			amp=velocity/127;
			file1=file1++"1.";
			file2=file2++"1.";
		},{
			// gather the velocity indices that are available
			// TODO: make this specific to a single note?
			velIndices=Array.fill(dyns,{ arg i;
				i*128/(dyns-1)
			});
			velIndex=velIndices.indexOfGreaterThan(velocity)-1;
			vels=[velIndices[velIndex],velIndices[velIndex+1]];
			buf1mix=(1-((velocity-vels[0])/(vels[1]-vels[0])));
			// add dynamic
			file1=file1++(velIndex+1).asInteger.asString++".";
			file2=file2++(velIndex+2).asInteger.asString++".";
			// add dynamic max
			file1=file1++dyns.asString++".";
			file2=file2++dyns.asString++".";
			// add round robin
			file1=file1++(noteRoundRobins.at(noteClosest.asString++"."++(velIndex+1).asString).rand+1).asString++".0.wav";
			file2=file2++(noteRoundRobins.at(noteClosest.asString++"."++(velIndex+2).asString).rand+1).asString++".0.wav";
		});


		// check if buffer is loaded
		if (buf.at(file1).isNil,{
			Buffer.read(server,PathName(folder+/+file1).fullPath,action:{ arg b1;
				b1.postln;
				buf.put(file1,b1);
				if (buf.at(file2).isNil,{
					Buffer.read(server,PathName(folder+/+file2).fullPath,action:{ arg b1;
						b1.postln;
						buf.put(file2,b1);
						// play it!
						this.doPlay(noteOriginal,amp,file1,file2,buf1mix,rate);
					});
				},{
					// play it!
					this.doPlay(noteOriginal,amp,file1,file2,buf1mix,rate);
				});
			});
		},{
			if (buf.at(file2).isNil,{
				Buffer.read(server,PathName(folder+/+file2).fullPath,action:{ arg b1;
					b1.postln;
					buf.put(file2,b1);
					// play it!
					this.doPlay(noteOriginal,amp,file1,file2,buf1mix,rate);
				});
			},{
				// play it!
				this.doPlay(noteOriginal,amp,file1,file2,buf1mix,rate);
			});
		});



	}

	noteOff {
		arg note;
		if (syn.at(note).notNil,{
			syn.at(note).keysValuesDo({ arg k,v;
				if (v.isRunning,{
					v.set(\gate,0);
				},{
					syn.at(note).set(k,nil);
				})
			});
		});
	}

	noteFade {
		arg note;
		if (syn.at(note).notNil,{
			syn.at(note).keysValuesDo({ arg k,v;
				if (v.isRunning,{
					v.set(\fade_trig,1,\fade_time,params.at("fadetime"));
				},{
					syn.at(note).set(k,nil);
				})
			});
		});
	}


	doPlay {
		arg note,amp,file1,file2,buf1mix,rate;
		var notename=1000000.rand;
		[notename,note,amp,file1,file2,buf1mix,rate].postln;
		// check if sound is loaded and unload it
		if (syn.at(note).isNil,{
			syn.put(note,Dictionary.new());
		});
		this.noteFade(note);
		syn.at(note).put(notename,Synth("playx"++buf.at(file1).numChannels,[
			\out,0,
			\amp,params.at("amp"),
			\pan,params.at("pan"),
			\attack,params.at("attack"),
			\decay,params.at("decay"),
			\sustain,params.at("sustain"),
			\release,params.at("release"),
			\buf1,buf.at(file1),
			\buf2,buf.at(file2),
			\buf1mix,buf1mix,
			\rate,rate,
		]).onFree({
			"freeing "++notename;
			syn.at(note).put(notename,nil);
		}));
		NodeWatcher.register(syn.at(note).at(notename));
	}


}
