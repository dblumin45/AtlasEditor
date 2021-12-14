What is it:
	Standalone editor for Defold .atlas files.

	I decided to create this because I found Defold's built-in .atlas editor to be a bit lacking in terms of organization:
		- No way to nest and hide groups of animations or standalone images
		- No way to nest and hide sub-groups of images within the same animation
		- Need to manually count frames in an animation (oftentimes needed to sync things up with more complex animations)
		- Not sure what causes it but sometimes copying and pasting a large selection of images causes them to not maintain their original order
		- No click and drag to reorder animations (only key commands to shift up/down one at a time)

	Includes both standalone .jar file and source code. Should work with java 1.8+, but I've only tested on 14+

Features:
	Every feature from the built-in editor is here, with the exception of: 
		- Showing how textures will be packed/how large the atlas will be 
		- The built-in editor has a very useful search function that can make it easy to find and add images from different folders with similar keywords (maybe I'll work on replicating this in the future)
	
	Every change this editor makes to .atlas files is fully compatible with Defold, I've been using it for a couple months with no issues, 
	however you should still make backups of work you can't afford to lose, on the off chance there's some use case I never ran into that can cause issues. 
	My features all rely on parsing keywords with the '#' comment delimeter that Defold ignores, so there should be no conflict with or corruption of the format it expects.
	
	The new features this editor has mostly address my specific needs and issues with the built-in editor:
		- You can encapsulate animations and images in named groups:
			- the groups can be collapsed to organize and reduce clutter in atlases with a lot of animations/images, and will count the number of direct children.
		- You can encapsulate images within an animation in breakpoints:
			- breakpoints will tell you the number of frames they contain (useful when you're splitting animations up into sections and need to sync this up with your scripts, such as active frames of an attack)
			- press SPACE when a breakpoint is selected to loop over those frames, rather than the whole animation
		- click and drag to reorder any of these nested elements while maintaining appropriate hierarchy (pretty self-explanatory)
		
	Hotkeys:
		- Spacebar - toggle preview loop of selected animation/breakpoint
		- ctrl-C - copy selection 
		- ctrl-V - paste clipboard
		- ctrl-X - cut selection 
		- ctrl-Z - undo
		- ctrl-S - save