-- modulate for samples
--

local MusicUtil=require "musicutil"
local Formatters=require 'formatters'

local MxSamples={}

local MaxVoices=40
local delay_rates_names={"whole-note","half-note","quarter note","eighth note","sixteenth note","thirtysecond"}
local delay_rates={4,2,1,1/2,1/4,1/8,1/16}
local delay_last_clock=0
local velocities={}
velocities[1]={1,4,7,10,13,16,19,22,25,28,31,34,38,41,43,46,49,52,55,57,60,62,64,66,68,70,71,73,74,76,77,79,80,81,83,84,85,86,87,89,90,91,92,93,94,95,95,96,97,98,99,99,100,101,102,102,103,104,104,105,105,106,106,107,107,108,108,109,109,109,110,110,111,111,111,112,112,112,112,113,113,113,114,114,114,114,115,115,115,115,115,116,116,116,116,116,117,117,117,117,118,118,118,118,118,119,119,119,120,120,120,120,121,121,121,122,122,122,123,123,124,124,124,125,125,126,126,127}
velocities[2]={0,2,3,4,6,7,8,10,11,13,14,15,17,18,19,21,22,23,25,26,27,29,30,31,33,34,35,37,38,39,40,42,43,44,45,47,48,49,50,52,53,54,55,57,58,59,60,61,62,64,65,66,67,68,69,70,71,72,73,75,76,77,78,79,80,81,82,83,83,84,85,86,87,88,89,90,91,92,92,93,94,95,96,97,97,98,99,100,100,101,102,103,103,104,105,106,106,107,108,109,109,110,111,111,112,113,113,114,115,115,116,117,117,118,119,119,120,120,121,122,122,123,124,124,125,126,126,127}
velocities[3]={1,1,1,1,1,2,2,2,2,2,2,3,3,3,3,3,4,4,4,4,5,5,5,5,6,6,6,6,7,7,7,8,8,8,9,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16,17,18,18,19,20,20,21,22,23,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,42,43,44,45,47,48,49,51,52,54,55,57,58,60,62,63,65,66,68,70,72,73,75,77,79,80,82,84,86,88,90,92,94,95,97,99,101,103,105,107,109,111,113,115,117,119,121,123,125,127}
velocities[4]={}
for i=1,128 do
  table.insert(velocities[4],64)
end

function MxSamples:new(args)
  local l=setmetatable({},{__index=MxSamples})
  local args=args==nil and {} or args
  l.debug=args.debug

  -- lets add files
  l:list_instruments()

  -- add parameters
  params:add_group("MX.SAMPLES",21)
  local filter_freq=controlspec.new(20,20000,'exp',0,20000,'Hz')
  -- params:add_option("mxsamples_instrument","instrument",l.instruments)
  params:add {
    type='control',
    id="mxsamples_amp",
    name="amp",
  controlspec=controlspec.new(0,10,'lin',0.01,1.0,'amp',0.01/10)}
  params:add {
    type='control',
    id="mxsamples_pan",
    name="pan",
  controlspec=controlspec.new(-1,1,'lin',0,0)}
  params:add {
    type='control',
    id="mxsamples_attack",
    name="attack",
  controlspec=controlspec.new(0,10,'lin',0.01,0.01,'s',0.01/10)}
  params:add {
    type='control',
    id="mxsamples_decay",
    name="decay",
  controlspec=controlspec.new(0,10,'lin',0,1,'s')}
  params:add {
    type='control',
    id="mxsamples_sustain",
    name="sustain",
  controlspec=controlspec.new(0,2,'lin',0,0.9,'amp')}
  params:add {
    type='control',
    id="mxsamples_release",
    name="release",
  controlspec=controlspec.new(0,10,'lin',0,0.2,'s')}
  params:add {
    type='control',
    id="mxsamples_transpose_midi",
    name="transpose midi",
  controlspec=controlspec.new(-24,24,'lin',0,0,'note',1/48)}
  params:add {
    type='control',
    id="mxsamples_transpose_sample",
    name="transpose sample",
  controlspec=controlspec.new(-24,24,'lin',0,0,'note',1/48)}
  params:add {
    type='control',
    id="mxsamples_tune",
    name="tune sample",
  controlspec=controlspec.new(-100,100,'lin',0,0,'cents',1/200)}
  params:add {
    type='control',
    id='mxsamples_lpf_mxsamples',
    name='low-pass filter',
    controlspec=filter_freq,
    formatter=Formatters.format_freq
  }
  params:add {
    type='control',
    id="mxsamples_lpfrq_mxsamples",
    name="hpf rq",
  controlspec=controlspec.new(0.01,1,'lin',0.01,1,'',0.01/1)}
  params:add {
    type='control',
    id='mxsamples_hpf_mxsamples',
    name='high-pass filter',
    controlspec=controlspec.new(20,20000,'exp',0,20,'Hz'),
    formatter=Formatters.format_freq
  }
  params:add {
    type='control',
    id="mxsamples_hpfrq_mxsamples",
    name="hpf rq",
  controlspec=controlspec.new(0.01,1,'lin',0.01,1,'',0.01/1)}
  params:add {
    type='control',
    id="mxsamples_reverb_send",
    name="reverb send",
  controlspec=controlspec.new(0,100,'lin',0,0,'%',1/100)}
  params:add {
    type='control',
    id="mxsamples_delay_send",
    name="delay send",
  controlspec=controlspec.new(0,100,'lin',0,0,'%',1/100)}
  params:add {
    type='control',
    id="mxsamples_delay_times",
    name="delay iterations",
  controlspec=controlspec.new(0,100,'lin',0,1,'beats',1/100)}
  params:set_action("mxsamples_delay_times",function(x)
    if engine.name=="MxSamples" then
      --   engine.mxsamples_delay_feedback(x/100)
    end
  end)
  params:add_option("mxsamples_delay_rate","delay rate",delay_rates_names,1)
  params:set_action("mxsamples_delay_rate",function(x)
    if engine.name=="MxSamples" then
      --   engine.mxsamples_delay_beats(delay_rates[x])
    end
  end)
  params:add {
    type='control',
    id="mxsamples_sample_start",
    name="sample start",
  controlspec=controlspec.new(0,1000,'lin',0,0,'ms',1/1000)}
  params:add {
    type='control',
    id="mxsamples_play_release",
    name="play release prob",
  controlspec=controlspec.new(0,100,'lin',0,0,'%',1/100)}
  params:add_option("mxsamples_scale_velocity","velocity sensitivity",{"delicate","normal","stiff","fixed"},2)
  params:add_option("mxsamples_pedal_mode","pedal mode",{"sustain","sostenuto"},1)

  params:default()

  return l
end

function MxSamples:on(mx)
  if mx.name==nil then
    print("MxSamples:on error: no name")
    do return end
  end
  if not util.file_exists(mx.name) then
    mx.name=_path.audio.."mx.samples/"..mx.name
    if not util.file_exists(mx.name) then
      print("MxSamples:on error: name is not folder")
      do return end
    end
  end
  if mx.midi==nil then
    print("MxSamples:on error: no midi")
    do return end
  end
  mx.velocity=mx.velocity or 60

  -- scale velocity depending on sensitivity
  if params:get("mxsamples_scale_velocity")<4 then
    mx.velocity=velocities[params:get("mxsamples_scale_velocity")][math.floor(mx.velocity+1)]
  end

  if mx.on==nil then
    mx.on=true
  end

  mx.amp=mx.amp or params:get("mxsamples_amp")
  mx.pan=mx.pan or params:get("mxsamples_pan")
  mx.attack=mx.attack or params:get("mxsamples_attack")
  mx.decay=mx.decay or params:get("mxsamples_decay")
  mx.sustain=mx.sustain or params:get("mxsamples_sustain")
  mx.release=mx.release or params:get("mxsamples_release")
  mx.lpf=mx.lpf or params:get("mxsamples_lpf_mxsamples")
  mx.lpfrq=mx.lpfrq or params:get("mxsamples_lpfrq_mxsamples")
  mx.hpf=mx.hpf or params:get("mxsamples_hpf_mxsamples")
  mx.hpfrq=mx.hpfrq or params:get("mxsamples_hpfrq_mxsamples")
  mx.midi=mx.midi+params:get("mxsamples_transpose_midi")

  mx.delay_send=mx.delay_send or params:get("mxsamples_delay_send")/100
  mx.reverb_send=mx.reverb_send or params:get("mxsamples_reverb_send")/100

  if mx.on then
    engine.mx_note_onfx(mx.name,mx.midi,mx.velocity,
      mx.amp,mx.pan,mx.attack,mx.decay,mx.sustain,mx.release,
    mx.delay_send,mx.reverb_send,mx.lpf,mx.lpfrq,mx.hpf,mx.hpfrq)
  else
    engine.mx_note_off(mx.name,mx.midi)
  end
end

function MxSamples:off(mx)
  mx.on=false
  mx.velocity=0
  self:on(mx)
end

function MxSamples:list_instruments()
  self.instruments={}
  for _,name in ipairs(util.scandir(_path.audio.."mx.samples")) do
    if name:sub(-1,-1)=="/" then
      table.insert(self.instruments,name:sub(1,-2))
    end
  end
  return self.instruments
end

return MxSamples
