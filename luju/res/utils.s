extern __exception
extern java.lang.Object
extern java.lang.Object$vt

section .text

; check if index ebx is valid for array at eax
	global __arrayBoundCheck
__arrayBoundCheck:
	cmp 	[eax+8], ebx
	jle 	.outOfBounds
	ret
.outOfBounds:
	call	__exception
	
	global __divideCheck
__divideCheck:
	test 	ebx, ebx
	je  	.divisionByZero
	ret
.divisionByZero:
	call	__exception
	
; eax = address to zero, ebx = length (in DWORDs)
; note that the actual length in bytes is ebx * 4 + 4 (for the length field)
	global __zeroArray
__zeroArray:
	push	eax
	push	ecx
	push	edx
	mov		ecx, ebx
	add 	ecx, 3
	xor 	edx, edx
.loop:
	mov 	[eax], edx
	add     eax, 4
	sub		ecx, 1
	jnz 	.loop
	pop 	edx
	pop 	ecx
	pop 	eax
	ret
	
	; check if type eax is of type ebx
	global __instanceOf
__instanceOf:
	push	ebp
	mov 	ebp, esp
	
	push 	0
	mov 	eax, [ebp+12]
	push	eax
	mov 	eax, [ebp+8]
	push	eax
	call	__checkCast
	add 	esp, 12
	cmp 	eax, -1
	jne 	.true
	mov 	eax, 0
	jmp 	.exit
.true:
	mov 	eax, 1
.exit:
	mov 	esp, ebp
	pop 	ebp
	ret
	
; check if type eax is of type ebx
	global __checkCast
__checkCast:
	; ebp+8 = a
	; ebp+12 = b
	; ebp+16 = offset
	push	ebp
	mov 	ebp, esp
	sub 	esp, 16 ; create 5 local vars
					; -4 = t
					; -8 = i
					; -12 = aa
					; -16 = index
	
	mov 	eax, [ebp+8]
	cmp 	eax, [ebp+12]
	jne 	.start
	mov 	eax, [ebp+16]
	jmp 	.exit
	
.start:
	mov 	dword [ebp-16], 0
	mov 	eax, [ebp+8]
	lea 	ecx, [eax+8]
	mov 	[ebp-4], ecx
	
	mov 	dword [ebp-8], 0
	jmp .forCond
.forUpdate:
	mov 	eax, dword [ebp-8]
	add 	eax, 1
	mov 	dword [ebp-8], eax
.forCond:
	mov 	eax, [ebp+8]
	mov 	ecx, [ebp-8]
	cmp 	ecx, dword [eax+4]
	jge 	.false
	
	; A* aa = (A*)(*T++);
	mov 	eax, [ebp-4]
	mov 	ecx, [eax]
	mov 	dword [ebp-12], ecx
	add 	eax, 4
	mov 	dword [ebp-4], eax
	
	; int val = instanceOf(aa, b, offset + index);
	mov 	eax, dword [ebp+16]
	add 	eax, dword [ebp-16]
	push	eax
	mov 	ecx, dword [ebp+12]
	push	ecx
	mov 	edx, dword [ebp-12]
	push 	edx
	call	__checkCast
	add 	esp, 12
	
	;if (val != -1) return val;
	cmp 	eax, 0FFFFFFFFh
	je  	.continue
	jmp 	.exit
	
.continue:
	; index = *t++;
	mov 	eax, [ebp-4]
	mov     ecx, [ebp-16]
	add 	ecx, [eax]
	mov 	dword [ebp-16], ecx
	add 	eax, 4
	mov 	dword [ebp-4], eax
	
	jmp 	.forUpdate
.false: 
	or  	eax, 0FFFFFFFFh
.exit:
	mov 	esp, ebp
	pop 	ebp
	ret
	
; define primitive array class structure
	global int#Array
int#Array:
	dd	java.lang.Object$vt
	dd	1
	dd	java.lang.Object
	dd	0
	global short#Array
short#Array:
	dd	java.lang.Object$vt
	dd	1
	dd	java.lang.Object
	dd	0
	global char#Array
char#Array:
	dd	java.lang.Object$vt
	dd	1
	dd	java.lang.Object
	dd	0
	global boolean#Array
boolean#Array:
	dd	java.lang.Object$vt
	dd	1
	dd	java.lang.Object
	dd	0
	global byte#Array
byte#Array:
	dd	java.lang.Object$vt
	dd	1
	dd	java.lang.Object
	dd	0
	
	global null
null:
	dd	0
	dd	0
	
	