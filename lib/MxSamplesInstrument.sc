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

	var <busDelay;
	var <busReverb;

	var pedalSustainOn;
	var pedalSostenutoOn;
	var pedalSustainNotes;
	var pedalSostenutoNotes;
	var voicesOn;

	*new {
		arg serverName,folderToSamples,numberMaxSamples,busDelayArg,busReverbArg;
		^super.new.init(serverName,folderToSamples,numberMaxSamples,busDelayArg,busReverbArg);
	}

	init {
		arg serverName,folderToSamples,numberMaxSamples,busDelayArg,busReverbArg;

		server=serverName;
		folder=folderToSamples;
		maxSamples=numberMaxSamples;
		busDelay=busDelayArg;
		busReverb=busReverbArg;

		pedalSustainOn=false;
		pedalSostenutoOn=false;
		voicesOn=Dictionary.new();
		pedalSustainNotes=Dictionary.new();
		pedalSostenutoNotes=Dictionary.new();
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
			"delaysend",0.0,
			"reverbsend",0.0,
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
			DetectSilence.ar(snd,0.00001,doneAction:2);
			snd=Pan2.ar(snd,pan,amp);
			Out.ar(out,snd);
			Out.ar(busReverb,snd*sendReverb);
			Out.ar(busDelay,snd*sendDelay);
		}).send(server);

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

	// garbageCollect {
	// 	Routine {
	// 		loop {
	// 			1.wait;
	// 			while ({buf.size>maxSamples},{
	// 				var toRemove=buf.keys.asArray[0];
	// 				("removing "++toRemove).postln;
	// 				buf.put(toRemove,nil);
	// 			});
	// 		}
	// 	}.play;
	// }

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

	sustain {
		arg on;
		if (on==0,{
			// release all sustained notes
			pedalSustainNotes.keysValuesDo({ arg note, val;
				if (voicesOn.at(note)==nil,{
					pedalSustainNotes.removeAt(note);
					noteOff(note);
				});
			});
		}, {
			// add currently down notes to the pedal
			voicesOn.keysValuesDo({ arg note, val;
				pedalSustainNotes.put(note,1);
			});
		});
	}


	sostenuto {
		arg on;
		if (pedalSostenutoOn==false,{
			// release all sustained notes
			pedalSostenutoNotes.keysValuesDo({ arg note, val;
				if (voicesOn.at(note)==nil,{
					pedalSostenutoNotes.removeAt(note);
					noteOff(note);
				});
			});
		},{
			// add currently held notes
			voicesOn.keysValuesDo({ arg note, val;
				pedalSostenutoNotes.put(note,1);
			});
		});
	}

	noteOff {
		arg note;
		var keys;
		voicesOn.removeAt(note);
		if (pedalSustainOn==true,{
			pedalSustainNotes.put(note,1);
		},{
			if ((pedalSostenutoOn==true)&&(pedalSostenutoNotes.at(note)!=nil),{
				// do nothing, it is a sostenuto note
			},{
				// remove the sound
				if (syn.at(note).notNil,{
					keys=syn.at(note).keys.asArray;
					keys.do({ arg k,i;
						var v=syn.at(note).at(k);
						if (v.notNil,{
							if (v.isRunning,{
								if (v.isPlaying,{
									syn.at(note).removeAt(k);
									v.set(\gate,0,\release,params.at("fadetime"));
								});
							});
						});
					});
				});
			});
		});

	}

	noteFade {
		arg note;
		var keys;
		if (syn.at(note).notNil,{
			keys=syn.at(note).keys.asArray;
			keys.do({ arg k,i;
				var v=syn.at(note).at(k);
				if (v.notNil,{
					if (v.isRunning,{
						if (v.isPlaying,{
							syn.at(note).removeAt(k);
							v.set(\gate,0,\release,params.at("fadetime"));
						});
					});
				});
			});
		});
	}


	doPlay {
		arg note,amp,file1,file2,buf1mix,rate;
		var notename=1000000.rand;
		var node;
		//[notename,note,amp,file1,file2,buf1mix,rate].postln;
		// check if sound is loaded and unload it
		if (syn.at(note).isNil,{
			syn.put(note,Dictionary.new());
		});
		this.noteFade(note);
		node=Synth.head(server,"playx"++buf.at(file1).numChannels,[
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
			\busDelay,busDelay,
			\sendDelay,params.at("delaysend"),
			\busReverb,busReverb,
			\sendReverb,params.at("reverbsend"),
		]).onFree({
			syn.at(note).removeAt(notename);
		});
		syn.at(note).put(notename,node);
		voicesOn.put(note,1);
		NodeWatcher.register(node,true);
	}


	free {
		syn.keysValuesDo({arg note,v1;
			syn.at(note).keysValuesDo({ arg k,v;
				v.free;
			});
		});
		buf.keysValuesDo({ arg name,b;
			b.free;
		});
	}

}
