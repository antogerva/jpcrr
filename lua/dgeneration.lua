
on_redraw = function()
	-- Make some empty space to right, so we don't have to write over game graphics.
	-- The first parameter (1) controls when the empty space will appear. 1 => Screen only, 2 => Dump only
	-- 3 => Both.
	-- The second parameter (160) is size of the empty space. In this case, 160 pixels wide (20 characters).
	jpcrr.hud.right_gap(1, 160);
	-- The x-position to place texts at. Usually this will be 640 ingame, but in case it isn't...
	pos = jpcrr.vga_resolution();

	-- Write some text.
	-- 1st parameter (1): When the empty space will appear (same meanings as first argument of right_gap).
	-- 2nd parameter (pos): x-position to write the text to.
	-- 3nd parameter (0): y-position to write the text to.
	-- 4th parameter ("ypos=" .. jpcrr.read_byte(0x0x2B0D8)): The text to write.
	-- 5th parameter (false): If true, linefeed in text causes line change in printed text.
	-- 6th parameter (255): Foreground red (0-255).
	-- 7th parameter (255): Foreground green (0-255).
	-- 8th parameter (255): Foreground blue (0-255).
	-- 9th parameter (255): Foreground alpha (0-255).
	-- 10th parameter (0): Background red (0-255).
	-- 11th parameter (0): Background green (0-255).
	-- 12th parameter (0): Background blue (0-255).
	-- 13th parameter (0): Background alpha (0-255).
	jpcrr.hud.chargen(1, pos, 0, "ypos=" .. jpcrr.read_byte(0x2B0D8), false, 255, 255, 255, 255, 0, 0, 0, 0);
	-- Write another text (in this case, number of current frame). The text height is 16, so write it at
	-- y-position 0 + 16 = 16.
	jpcrr.hud.chargen(1, pos, 16, "frame=" ..  jpcrr.frame_number(), false, 255, 255, 255, 255, 0, 0, 0, 0);
end

-- Register on_redraw to run on each frame received.
jpcrr.register_redraw_function(on_redraw);
-- Wait for events.
while true do jpcrr.wait_event() end
