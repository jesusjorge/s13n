import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Base64

//https://github.com/jesusjorge/s13n/wiki/1.1
//
//var ByteArrayResult = CUInt.Write(TrivialULong? )
//var ULongResult = CUInt.Read(ByteArrayInputStream)


enum class CUInts{ CUInt8, CUInt16, CUInt32, CUInt64, CUInt128 }

class CUInt {
    companion object{
        val DefaultType = CUInts.CUInt128
        fun Read(Input: InputStream, Type: CUInts = DefaultType) : ULong?{
            var Buffer = ByteArray(8)
            var Read = Input.read(Buffer,0,1)
            if(Read == 0)
                return null
            var Result = Read(Buffer[0],Type)
            if(Result.PendingRead == 0)
                return Result.Value
            Read = Input.read(Buffer,0,Result.PendingRead)
            if(Read < Result.PendingRead)
                return null
            return ReadBytes(Buffer,Result.PendingRead)
        }
        fun Write(Value: ULong?, Type: CUInts = DefaultType) : ByteArray{
            if(Value == null)
                return byteArrayOf(255.toByte())
            else if(Value <= MaxHeaderValue(Type))
                return byteArrayOf(Value.toByte())
            var PendingBytes = 0
            if(Value <= UShort.MAX_VALUE.toULong())
                PendingBytes = 2
            else if(Value <= UInt.MAX_VALUE.toULong())
                PendingBytes = 4
            else if(Value <= ULong.MAX_VALUE.toULong())
                PendingBytes = 8
            ByteBuffer.allocate(PendingBytes + 1).apply {
                order(ByteOrder.BIG_ENDIAN)
                if(PendingBytes == 2) {
                    put(254.toByte())
                    putShort(Value.toUShort().toShort())
                }
                else if(PendingBytes == 4) {
                    put(253.toByte())
                    putInt(Value.toUInt().toInt())
                }
                else if(PendingBytes == 8) {
                    put(252.toByte())
                    putLong(Value.toULong().toLong())
                }
                flip()
                var Result = array()
                return Result
            }
        }
        fun ReadBytes(LeadingBytes: ByteArray) : ULong{
            return ReadBytes(LeadingBytes,LeadingBytes.size)
        }
        fun ReadBytes(LeadingBytes: ByteArray, Length : Int) : ULong{
            var Buffer = byteArrayOf(0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00)
            LeadingBytes.copyInto(Buffer,8-Length,0,Length)
            ByteBuffer.wrap(Buffer).apply {
                order(ByteOrder.BIG_ENDIAN)
                return long.toULong()
            }
        }
        fun Read(HeaderByte: Byte, Type: CUInts = DefaultType) : CUIntReadResult{
            val Pending = PendingRead(HeaderByte,Type)
            if(Pending == null)
                return CUIntReadResult(null,0)
            else if(Pending == 0)
                return CUIntReadResult(HeaderByte.toUByte().toULong(),0)
            else
                return CUIntReadResult(null, Pending)
        }
        //region [Internals]
        fun PendingRead(HeaderByte: Byte, Type : CUInts) : Int?{
            val Value = HeaderByte.toUByte().toULong()
            if(Value <= MaxHeaderValue(Type))
                return 0
            return ReadToken(HeaderByte)
        }
        fun MaxHeaderValue(Type: CUInts) : ULong{
            if(Type == CUInts.CUInt8)
                return 254UL
            else if(Type == CUInts.CUInt16)
                return 253UL
            else if(Type == CUInts.CUInt32)
                return 252UL
            else if(Type == CUInts.CUInt64)
                return 251UL
            else if(Type == CUInts.CUInt128)
                return 250UL
            throw Exception("Unknown CUInt type")
        }
        fun ReadToken(Value: Byte) : Int?{
            if(Value == 255.toByte())
                return null
            else if(Value == 254.toByte())
                return 2
            else if(Value == 253.toByte())
                return 4
            else if(Value == 252.toByte())
                return 8
            else if(Value == 251.toByte())
                return 16
            throw Exception("Unknown CUInt token")
        }
        fun SelfTest() : Boolean{
            var Log = StringBuilder()
            Log.append("CUInt did not pass the self check.\n\tTest Results:\n")
            var TestCases = "Ff8AAQID+fr+APv+AP3+AP7+AP/+AQD+AQH9AP///v0A/////QEAAAD9AQAAAf3////+/f/////8AAAAAQAAAAD8AAAAAQAAAAE="
            var TS = ByteArrayOutputStream()
            var RS = ByteArrayInputStream(Base64.decode(TestCases, Base64.DEFAULT))
            var Tests = Read(RS)!!
            TS.write(Write(Tests))
            while(Tests > 0UL){
                var Value = Read(RS)
                Log.append(Value.toString()).append("\n")
                TS.write(Write(Value))
                Tests = Tests - 1UL
            }
            var Results = String(Base64.encode(TS.toByteArray(),Base64.DEFAULT)).replace("\n","")
            if(Results != TestCases){
                Log.append("Source Test String: ").append(TestCases)
                Log.append("\nResult Test String: ").append(Results)
                throw Exception(Log.toString())
            }
            return Results == TestCases
        }
        //endregion
    }
}

data class CUIntReadResult(val Value: ULong?, val PendingRead: Int)
