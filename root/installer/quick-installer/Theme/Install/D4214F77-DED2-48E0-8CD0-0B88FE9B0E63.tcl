proc CreateWindow.D4214F77-DED2-48E0-8CD0-0B88FE9B0E63 {wizard id} {
   
    
    set base  [$wizard widget get $id]
    set frame $base.titleframe

    grid rowconfigure    $base 3 -weight 1
    grid columnconfigure $base 0 -weight 1

    frame $frame -bd 0 -relief flat -background white
    grid  $frame -row 0 -column 0 -sticky nsew

    grid rowconfigure    $frame 1 -weight 1
    grid columnconfigure $frame 0 -weight 1

    Label $frame.title -background white -anchor nw -justify left -autowrap 1  -font TkCaptionFont -textvariable [$wizard variable $id -text1]
    grid $frame.title -row 0 -column 0 -sticky new -padx 5 -pady 5
    $id widget set Title -widget $frame.title

    Label $frame.subtitle -background white -anchor nw -autowrap 1  -justify left -textvariable [$wizard variable $id -text2]
    grid $frame.subtitle -row 1 -column 0 -sticky new -padx [list 20 5]
    $id widget set Subtitle -widget $frame.subtitle

    label $frame.icon -borderwidth 0 -background white -anchor c
    grid  $frame.icon -row 0 -column 1 -rowspan 2
    $id widget set Icon -widget $frame.icon -type image

    Separator $base.separator -relief groove -orient horizontal
    grid $base.separator -row 1 -column 0 -sticky ew 

    Label $base.caption -anchor nw -justify left -autowrap 1  -textvariable [$wizard variable $id -text3]
    grid $base.caption -row 2 -sticky nsew -padx 8 -pady [list 8 4]
    $id widget set Caption -widget $base.caption

    frame $base.clientarea
    grid  $base.clientarea -row 3 -sticky w -padx 8 -pady 20
    $id widget set ClientArea -widget $base.clientarea -type frame

    grid rowconfigure    $base.clientarea 6 -weight 1
    grid columnconfigure $base.clientarea 2 -weight 1

    ttk::label $base.clientarea.label2 -text "Admin Password:" -justify right
    grid $base.clientarea.label2 -row 1 -column 0 -sticky e

    ttk::entry $base.clientarea.entry2 -textvariable ::info(ADMIN_PWD) -show "*"
    grid $base.clientarea.entry2 -row 1 -column 1 -sticky e

    ttk::label $base.clientarea.label3 -text "Repeat Password:" -justify right
    grid $base.clientarea.label3 -row 2 -column 0 -sticky e

    ttk::entry $base.clientarea.entry3 -textvariable ::info(ADMIN_PWD2) -show "*"
    grid $base.clientarea.entry3 -row 2 -column 1 -sticky e

    Label $base.message -anchor nw -justify left -autowrap 1  -textvariable [$wizard variable $id -text4]
    grid $base.message -row 4 -sticky nsew -padx 8 -pady [list 4 8]
    $id widget set Message -widget $base.message
}

