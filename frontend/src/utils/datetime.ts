import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

dayjs.extend(utc)
dayjs.extend(timezone)

export function toLocalDateTimeString(input: string | Date) {
  return dayjs(input).format('YYYY-MM-DDTHH:mm:ss')
}

export function formatRange(start: string, end: string) {
  return `${dayjs.tz(start, 'Asia/Shanghai').format('YYYY-MM-DD HH:mm')} - ${dayjs.tz(end, 'Asia/Shanghai').format('HH:mm')}`
}

export function isPast(input: string | Date) {
  return dayjs.tz(dayjs(input), 'Asia/Shanghai').isBefore(dayjs().tz('Asia/Shanghai'))
}

