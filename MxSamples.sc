MxSamples {

	var server;
	var maxSamples;

	var <ins;


	*new {
		arg serverName,numberMaxSamples;
		^super.new.init(serverName,numberMaxSamples);
	}

	init {
		arg serverName,numberMaxSamples;

		ins=Dictionary.new();
	}


	noteOn {
		arg folder,note,velocity;
		if (ins.at(folder).isNil,{
			ins.put(folder,MxSamplesInstrument(server,folder,maxSamples));
		});
		ins.at(folder).noteOn(note,velocity);

	}

	noteOff {
		arg folder,note;
		if (ins.at(folder).notNil,{
			ins.at(folder).noteOff(note);
		});
	}

}
