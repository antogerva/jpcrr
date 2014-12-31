
on_redraw = function()
	pos = jpcrr.vga_resolution();
end

-- Register on_redraw to run on each frame received.
jpcrr.register_redraw_function(on_redraw);


while true do 
	print(jpcrr.mouse_state())
	eventtype, eventmessage = jpcrr.wait_event();
end