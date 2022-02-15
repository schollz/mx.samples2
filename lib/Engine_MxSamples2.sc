// Engine_MxSamples2

// Inherit methods from CroneEngine
Engine_MxSamples2 : CroneEngine {

	// <mxsamples2>
    var mx;
	// </mxsamples2>

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		// <mxsamples2>
        mx=MxSamples(Server.default,100);

		this.addCommand("mx_note_on","sff", { arg msg;
			mx.noteOn(msg[1].asString,msg[2],msg[3]);
		});
        
		this.addCommand("mx_note_off","sf", { arg msg;
			mx.noteOff(msg[1].asString,msg[2]);
		});

        // </mxsamples2>
	}

	free {
		// <mxsamples2>
        mx.free;
        // </mxsamples2>
	}
}