(
s.waitForBoot({
	// ~mx=MxSamples(s,"/home/zns/Documents/mx.samples2/kalimba");
	~mx=MxSamples(s,"/home/zns/Documents/mx.samples2/steinway_model_b",400);
	~mx.garbageCollect;
	"ready".postln;
	// ~mx.play(58,1);
	~mx.noteOn(62,60);

});
)
	~mx.noteOn(62-24,60);
	~mx.noteOn(62+4,60);
	~mx.noteOn(62+7,120);
	~mx.noteOn(62+7+24,30);

	~mx.noteOff(62,3);

~mx.buf.postln;
~mx.play(70,120);

~mx.play(21,70);
~mx.play(78,1);

~mx.buf.size
a=Dictionary.new();
a.put(1,2);
a.put(3,4);
a.postln;
a.size.postln;
a.put(1,nil);
a.keys.asArray[0]
(
var noteDynamics=Dictionary.new();
var noteRoundRobins=Dictionary.new();
var noteNumbers=Array.new(128);
var note=73;
var noteClosest;
PathName.new("/home/zns/Documents/mx.samples2/kalimba").entries.do({ arg v;
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
			if (noteRoundRobins.at(note).isNil,{
				noteRoundRobins.put(note,rr);
			},{
				if (rr>noteRoundRobins.at(note),{
					noteRoundRobins.put(note,rr);
				});
			});
		});
	});
});
noteNumbers=noteNumbers.sort;

noteRoundRobins.postln;
noteRoundRobins.at(74).rand.postln;

"done".postln;
)

(5.0.asInteger.asString++"hi").postln;

a=Scale.chromatic.ratios;

a=Array.new(128);

[2, 3, 5, 6].indexIn(5.2);
a.add(1)

(3.rand+1)


(
var vels=4;
var velocity=30;
var buf1mix;
var velIndex;
var velIndices=Array.fill(vels,{ arg i;
	i*128/(vels-1)
});
velIndices.postln;
velIndex=velIndices.indexOfGreaterThan(velocity)-1;
vels=[velIndices[velIndex],velIndices[velIndex+1]];
buf1mix=(1-((velocity-vels[0])/(vels[1]-vels[0]))).postln;
[velIndex,velIndex+1].postln;
"ok".postln;
)