enum import Enum
import io
import struct

#https://github.com/jesusjorge/s13n/wiki/1.1
#
#var ByteArrayResult = CUInt.Write(TrivialULong)
#var ULongResult = CUInt.Read(ByteArrayInputStream)

class CUInts(Enum):
    CUInt8 = 1
    CUInt16 = 2
    CUInt32 = 4
    CUInt64 = 8
    CUInt128 = 16

CUInt_DefaultType = CUInts.CUInt128

@DCLS.dataclass(unsafe_hash=True)
class CUIntReadResult:
    Value: int
    PendingRead: int

class CUInt:
    def Write(Value,CUIntType=CUInt_DefaultType):
        if Value == None:
            return b'\xff'
        elif Value <= CUInt.MaxHeaderValue(CUIntType):
            return bytes([Value])
        elif Value <= 65535:
            return b'\xfe' + Value.to_bytes(2,"big")
        elif Value <= 4294967295:
            return b'\xfd' + Value.to_bytes(4,"big")
        elif Value <= 18446744073709551615:
            return b'\xfc' + Value.to_bytes(8,"big")
        elif Value <= 340282366920938463463374607431768211455:
            return b'\xfb' + Value.to_bytes(16,"big")
        else:
            raise Exception("CUInt value too large on this implementation")
    def Read(BStream,CUIntType=CUInt_DefaultType):
        B = CUInt.ReadHeader(BStream.read(1),CUIntType)
        if B.PendingRead == 0:
            return B.Value
        return CUInt.ReadLeading(BStream.read(B.PendingRead))
    def ReadLeading(LeadingBytes,Length = None):
        if Length == None:
            Length = len(LeadingBytes)
        return int.from_bytes(LeadingBytes[0:Length],"big")
    def ReadHeader(B,CUIntType=CUInt_DefaultType):
        B = int.from_bytes(B,"big")
        PR = CUInt.PendingRead(B,CUIntType)
        if PR == None:
            return CUIntReadResult(None,0)
        elif PR == 0:
            return CUIntReadResult(int(B),0)
        else:
            return CUIntReadResult(None,PR)
    ###INTERNALS### 
    def PendingRead(B,CUIntType=CUInt_DefaultType):
        if B <= CUInt.MaxHeaderValue(CUIntType):
            return 0
        return CUInt.ReadToken(B)
    def MaxHeaderValue(CUIntType=CUInt_DefaultType):
        if CUIntType == CUInts.CUInt8:
            return 254
        elif CUIntType == CUInts.CUInt16:
            return 253
        elif CUIntType == CUInts.CUInt32:
            return 252
        elif CUIntType == CUInts.CUInt64:
            return 251
        elif CUIntType == CUInts.CUInt128:
            return 250
        raise Exception("Unknown CUInt type")
    def ReadToken(B):
        if B == 0xff:
            return None
        elif B == 0xfe:
            return 2
        elif B == 0xfd:
            return 4
        elif B == 0xfc:
            return 8
        elif B == 0xfb:
            return 16
        raise Exception("Unknown CUInt type")
